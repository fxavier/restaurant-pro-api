package com.restaurantpos.identityaccess.service;

import com.restaurantpos.identityaccess.dto.AuthResponse;
import com.restaurantpos.identityaccess.entity.RefreshToken;
import com.restaurantpos.identityaccess.entity.User;
import com.restaurantpos.identityaccess.exception.AuthenticationException;
import com.restaurantpos.identityaccess.repository.RefreshTokenRepository;
import com.restaurantpos.identityaccess.repository.UserRepository;
import com.restaurantpos.identityaccess.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user authentication operations.
 * Handles login, token refresh, and logout.
 * 
 * Requirements: 2.2, 2.3, 2.8
 */
@Service
@Transactional
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final long refreshTokenExpiryDays;
    
    public AuthenticationService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenExpiryDays = 7; // Default from requirements
    }
    
    /**
     * Authenticates a user and generates access and refresh tokens.
     * 
     * @param tenantId the tenant ID
     * @param username the username
     * @param password the password
     * @return authentication response with tokens
     * @throws AuthenticationException if authentication fails
     */
    public AuthResponse login(UUID tenantId, String username, String password) {
        logger.debug("Login attempt for user: {} in tenant: {}", username, tenantId);
        
        // Find user by tenant and username
        User user = userRepository.findByTenantIdAndUsername(tenantId, username)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));
        
        // Check if user is active
        if (!user.isActive()) {
            logger.warn("Login attempt for inactive user: {}", username);
            throw new AuthenticationException("User account is inactive");
        }
        
        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            logger.warn("Invalid password for user: {}", username);
            throw new AuthenticationException("Invalid username or password");
        }
        
        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getTenantId(),
                user.getRole().name()
        );
        
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(),
                user.getTenantId()
        );
        
        // Store refresh token
        storeRefreshToken(user.getId(), refreshToken);
        
        logger.info("User logged in successfully: {} in tenant: {}", username, tenantId);
        
        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getTenantId(),
                user.getRole().name()
        );
    }
    
    /**
     * Refreshes an access token using a valid refresh token.
     * 
     * @param refreshTokenValue the refresh token
     * @return authentication response with new access token
     * @throws AuthenticationException if refresh token is invalid
     */
    public AuthResponse refreshToken(String refreshTokenValue) {
        logger.debug("Token refresh attempt");
        
        try {
            // Validate refresh token
            Jwt jwt = jwtTokenProvider.validateToken(refreshTokenValue);
            
            // Check if it's a refresh token
            if (!jwtTokenProvider.isRefreshToken(jwt)) {
                throw new AuthenticationException("Invalid token type");
            }
            
            // Extract user ID and tenant ID
            UUID userId = jwtTokenProvider.extractUserId(jwt);
            
            // Check if refresh token exists and is valid
            String tokenHash = hashToken(refreshTokenValue);
            RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                    .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));
            
            if (!refreshToken.isValid()) {
                logger.warn("Attempt to use invalid refresh token for user: {}", userId);
                throw new AuthenticationException("Refresh token is expired or revoked");
            }
            
            // Get user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AuthenticationException("User not found"));
            
            // Check if user is active
            if (!user.isActive()) {
                logger.warn("Token refresh attempt for inactive user: {}", userId);
                throw new AuthenticationException("User account is inactive");
            }
            
            // Generate new access token
            String newAccessToken = jwtTokenProvider.generateAccessToken(
                    user.getId(),
                    user.getUsername(),
                    user.getTenantId(),
                    user.getRole().name()
            );
            
            logger.info("Token refreshed successfully for user: {}", userId);
            
            return new AuthResponse(
                    newAccessToken,
                    refreshTokenValue, // Return the same refresh token
                    user.getId(),
                    user.getUsername(),
                    user.getTenantId(),
                    user.getRole().name()
            );
            
        } catch (JwtException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            throw new AuthenticationException("Invalid refresh token");
        }
    }
    
    /**
     * Logs out a user by revoking their refresh token.
     * 
     * @param refreshTokenValue the refresh token to revoke
     */
    public void logout(String refreshTokenValue) {
        logger.debug("Logout attempt");
        
        try {
            String tokenHash = hashToken(refreshTokenValue);
            refreshTokenRepository.findByTokenHash(tokenHash)
                    .ifPresent(token -> {
                        token.revoke();
                        refreshTokenRepository.save(token);
                        logger.info("User logged out successfully: {}", token.getUserId());
                    });
        } catch (Exception e) {
            logger.warn("Error during logout: {}", e.getMessage());
            // Don't throw exception on logout failure
        }
    }
    
    /**
     * Revokes all refresh tokens for a user.
     * Used for security events like password change.
     * 
     * @param userId the user ID
     */
    public void revokeAllUserTokens(UUID userId) {
        logger.info("Revoking all tokens for user: {}", userId);
        refreshTokenRepository.revokeAllByUserId(userId);
    }
    
    /**
     * Stores a refresh token in the database.
     */
    private void storeRefreshToken(UUID userId, String tokenValue) {
        String tokenHash = hashToken(tokenValue);
        Instant expiresAt = Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS);
        
        RefreshToken refreshToken = new RefreshToken(userId, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);
    }
    
    /**
     * Hashes a token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
