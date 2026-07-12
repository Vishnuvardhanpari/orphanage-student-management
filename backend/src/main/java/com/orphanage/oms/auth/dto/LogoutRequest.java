package com.orphanage.oms.auth.dto;

/**
 * Logout request. Refresh token is optional when a valid Bearer access token is present.
 */
public record LogoutRequest(
        String refreshToken
) {
}
