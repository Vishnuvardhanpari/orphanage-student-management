package com.orphanage.oms.audit.dto;

import com.orphanage.oms.audit.enums.AuditAction;
import com.orphanage.oms.audit.enums.AuditModule;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit log row for API responses.
 */
public record AuditLogResponse(
        UUID id,
        AuditModule module,
        AuditAction action,
        UUID entityId,
        String description,
        String username,
        String ipAddress,
        Instant createdDate
) {
}
