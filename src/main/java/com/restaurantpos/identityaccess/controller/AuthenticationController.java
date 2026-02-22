package com.restaurantpos.identityaccess.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurantpos.identityaccess.dto.AuthResponse;
import com.restaurantpos.identityaccess.exception.AuthenticationException;
import com.restaurantpos.identityaccess.service.AuthenticationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
     * Registers a new user account.
     * 
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authenticationService.register(
                    request.tenantId(),
                    request.username(),
                    request.password(),
                    request.email(),
                    request.role()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (AuthenticationException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Request DTO for login.
     */
    public record LoginRequest(
            @NotNull(message = "Tenant ID is required")
            UUID tenantId,
            
            @NotBlank(message = "Username is required")
            @Size(max = 100, message = "Username must not exceed 100 characters")
            String username,
            
            @NotBlank(message = "Password is required")
            String password
    ) {}
    
    /**
     * Request DTO for token refresh.
     */
    public record RefreshRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}
    
    /**
     * Request DTO for logout.
     */
    public record LogoutRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}
    
    /**
     * Request DTO for registration.
     */
    public record RegisterRequest(
            @NotNull(message = "Tenant ID is required")
            UUID tenantId,
            
            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
            String username,
            
            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password,
            
            @Size(max = 255, message = "Email must not exceed 255 characters")
            String email,
            
            @NotBlank(message = "Role is required")
            String role
    ) {}
}
