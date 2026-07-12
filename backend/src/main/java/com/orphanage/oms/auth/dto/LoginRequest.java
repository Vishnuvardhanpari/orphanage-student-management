package com.orphanage.oms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Username/password login request.
 */
public record LoginRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 8, max = 128) String password
) {
}
