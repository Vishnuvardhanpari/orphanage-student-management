package com.orphanage.oms.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Google ID token exchange request.
 */
public record GoogleLoginRequest(
        @NotBlank String idToken
) {
}
