package com.orphanage.oms.auth.dto;

/**
 * Authentication token response.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
