package com.orphanage.oms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin-initiated password reset request.
 */
public record ResetPasswordRequest(
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {
}
