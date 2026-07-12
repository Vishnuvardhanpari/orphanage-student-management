package com.orphanage.oms.user.dto;

import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to create a pre-provisioned application user.
 */
public record CreateUserRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull RoleName role,
        @NotNull AuthProvider authProvider,
        @Size(min = 8, max = 128) String password
) {
}
