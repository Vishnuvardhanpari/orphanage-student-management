package com.orphanage.oms.user.dto;

import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import java.time.Instant;
import java.util.UUID;

/**
 * User management response DTO (never includes password hash).
 */
public record UserDetailResponse(
        UUID id,
        String username,
        String email,
        RoleName role,
        boolean enabled,
        AuthProvider authProvider,
        boolean accountNonLocked,
        Instant lastLoginAt,
        Instant createdDate,
        Instant updatedDate
) {
}
