package com.restaurantpos.identityaccess;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * REST controller for authentication operations.
 * Provides endpoints for login, token refresh, and logout.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    
    private final AuthenticationService authenticationService;
    
    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    
    /**
     * Authenticates a user and returns access and refresh tokens.
     * 
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authenticationService.login(
                    request.tenantId(),
                    request.username(),
                    request.password()
            );
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            logger.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    
    /**
     * Refreshes an access token using a valid refresh token.
     * 
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            AuthResponse response = authenticationService.refreshToken(request.refreshToken());
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    
    /**
     * Logs out a user by revoking their refresh token.
     * 
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Request DTO for login.
     */
    public record LoginRequest(
            @NotBlank UUID tenantId,
            @NotBlank String username,
            @NotBlank String password
    ) {}
    
    /**
     * Request DTO for token refresh.
     */
    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {}
    
    /**
     * Request DTO for logout.
     */
    public record LogoutRequest(
            @NotBlank String refreshToken
    ) {}
}
