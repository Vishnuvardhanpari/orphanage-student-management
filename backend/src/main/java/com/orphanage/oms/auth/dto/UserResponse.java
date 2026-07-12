package com.orphanage.oms.auth.dto;

import com.orphanage.oms.user.enums.RoleName;
import java.util.UUID;

/**
 * Authenticated user profile returned by auth endpoints.
 */
public record UserResponse(
        UUID id,
        String username,
        String email,
        RoleName role
) {
}
