package com.orphanage.oms.audit.service;

import com.orphanage.oms.audit.dto.AuditLogResponse;
import com.orphanage.oms.audit.entity.AuditLog;
import com.orphanage.oms.audit.enums.AuditAction;
import com.orphanage.oms.audit.enums.AuditModule;
import com.orphanage.oms.audit.mapper.AuditLogMapper;
import com.orphanage.oms.audit.repository.AuditLogRepository;
import com.orphanage.oms.audit.repository.AuditLogSpecifications;
import com.orphanage.oms.exception.ApiException;
import com.orphanage.oms.security.SecurityUtils;
import com.orphanage.oms.util.ClientIpResolver;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Records and queries immutable audit events.
 */
@Service
public class AuditService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdDate",
            "username",
            "module",
            "action");

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final ClientIpResolver clientIpResolver;

    public AuditService(
            AuditLogRepository auditLogRepository,
            AuditLogMapper auditLogMapper,
            ClientIpResolver clientIpResolver) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
        this.clientIpResolver = clientIpResolver;
    }

    /**
     * Records an audit event using the current security principal and request IP.
     */
    @Transactional
    public void record(AuditModule module, AuditAction action, UUID entityId, String description) {
        String username = SecurityUtils.currentUsername().orElse("system");
        record(module, action, entityId, description, username, clientIpResolver.resolveCurrent());
    }

    /**
     * Records an audit event with an explicit actor and IP (e.g. login before SecurityContext is set).
     */
    @Transactional
    public void record(
            AuditModule module,
            AuditAction action,
            UUID entityId,
            String description,
            String username,
            String ipAddress) {
        if (module == null || action == null) {
            throw new IllegalArgumentException("module and action are required");
        }
        if (!StringUtils.hasText(description)) {
            throw new IllegalArgumentException("description is required");
        }
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("username is required");
        }

        AuditLog log = AuditLog.builder()
                .module(module)
                .action(action)
                .entityId(entityId)
                .description(description.trim())
                .username(username.trim())
                .ipAddress(truncate(ipAddress, 45))
                .build();
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(
            String search,
            AuditModule module,
            AuditAction action,
            String username,
            UUID entityId,
            Instant from,
            Instant to,
            Pageable pageable) {
        validateSort(pageable);
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "from must be on or before to.");
        }
        return auditLogRepository
                .findAll(
                        AuditLogSpecifications.build(search, module, action, username, entityId, from, to),
                        pageable)
                .map(auditLogMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getById(UUID id) {
        return auditLogRepository
                .findById(id)
                .map(auditLogMapper::toResponse)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Not Found", "Audit log not found."));
    }

    private void validateSort(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Validation Error",
                        "Invalid sort property: " + order.getProperty());
            }
        }
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
