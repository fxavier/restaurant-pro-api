package com.restaurantpos.identityaccess.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Provider for JWT token generation and validation.
 * Handles access tokens (15 min expiry) and refresh tokens (7 days expiry).
 * Includes tenant_id in JWT claims for multi-tenancy support.
 * 
 * Requirements: 2.1, 2.2, 1.6
 */
@Component
public class JwtTokenProvider {
    
    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final long accessTokenExpiryMinutes;
    private final long refreshTokenExpiryDays;
    
    public JwtTokenProvider(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            @Value("${jwt.access-token-expiry-minutes:15}") long accessTokenExpiryMinutes,
            @Value("${jwt.refresh-token-expiry-days:7}") long refreshTokenExpiryDays) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }
    
    /**
     * Generates an access token for the given user.
     * Access tokens expire after 15 minutes.
     * 
     * @param userId the user ID
     * @param username the username
     * @param tenantId the tenant ID (null for super admins)
     * @param role the user role
     * @return the generated JWT access token
     */
    public String generateAccessToken(UUID userId, String username, UUID tenantId, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpiryMinutes, ChronoUnit.MINUTES);
        
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("restaurant-pos-saas")
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        
        // Only add tenant_id claim if not null (super admins have no tenant)
        if (tenantId != null) {
            claimsBuilder.claim(TENANT_ID_CLAIM, tenantId.toString());
        }
        
        JwtClaimsSet claims = claimsBuilder.build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
    
    /**
     * Generates a refresh token for the given user.
     * Refresh tokens expire after 7 days.
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID (null for super admins)
     * @return the generated JWT refresh token
     */
    public String generateRefreshToken(UUID userId, UUID tenantId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenExpiryDays, ChronoUnit.DAYS);
        
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("restaurant-pos-saas")
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(userId.toString())
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        
        // Only add tenant_id claim if not null (super admins have no tenant)
        if (tenantId != null) {
            claimsBuilder.claim(TENANT_ID_CLAIM, tenantId.toString());
        }
        
        JwtClaimsSet claims = claimsBuilder.build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
    
    /**
     * Validates a JWT token and returns the decoded JWT.
     * 
     * @param token the JWT token to validate
     * @return the decoded JWT
     * @throws JwtException if the token is invalid or expired
     */
    public Jwt validateToken(String token) {
        return jwtDecoder.decode(token);
    }
    
    /**
     * Extracts the user ID from a JWT token.
     * 
     * @param jwt the decoded JWT
     * @return the user ID
     */
    public UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
    
    /**
     * Extracts the tenant ID from a JWT token.
     * 
     * @param jwt the decoded JWT
     * @return the tenant ID
     */
    public UUID extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString(TENANT_ID_CLAIM);
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }
    
    /**
     * Extracts the username from a JWT token.
     * 
     * @param jwt the decoded JWT
     * @return the username, or null if not present
     */
    public String extractUsername(Jwt jwt) {
        return jwt.getClaimAsString("username");
    }
    
    /**
     * Extracts the role from a JWT token.
     * 
     * @param jwt the decoded JWT
     * @return the role, or null if not present
     */
    public String extractRole(Jwt jwt) {
        return jwt.getClaimAsString("role");
    }
    
    /**
     * Checks if a JWT token is a refresh token.
     * 
     * @param jwt the decoded JWT
     * @return true if the token is a refresh token, false otherwise
     */
    public boolean isRefreshToken(Jwt jwt) {
        String tokenType = jwt.getClaimAsString(TOKEN_TYPE_CLAIM);
        return REFRESH_TOKEN_TYPE.equals(tokenType);
    }
    
    /**
     * Checks if a JWT token is an access token.
     * 
     * @param jwt the decoded JWT
     * @return true if the token is an access token, false otherwise
     */
    public boolean isAccessToken(Jwt jwt) {
        String tokenType = jwt.getClaimAsString(TOKEN_TYPE_CLAIM);
        return ACCESS_TOKEN_TYPE.equals(tokenType);
    }
}
