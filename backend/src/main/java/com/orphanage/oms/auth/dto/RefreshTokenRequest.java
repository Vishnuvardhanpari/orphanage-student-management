package com.orphanage.oms.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh token rotation request.
 */
public record RefreshTokenRequest(
        @NotBlank String refreshToken
) {
}
