package com.restaurantpos.identityaccess.dto;

import java.util.UUID;

/**
 * Response DTO for authentication operations.
 * Contains access token, refresh token, and user information.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String username,
        UUID tenantId,
        String role
) {}
