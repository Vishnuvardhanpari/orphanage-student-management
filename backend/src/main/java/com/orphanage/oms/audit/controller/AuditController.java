package com.orphanage.oms.audit.controller;

import com.orphanage.oms.audit.dto.AuditLogResponse;
import com.orphanage.oms.audit.enums.AuditAction;
import com.orphanage.oms.audit.enums.AuditModule;
import com.orphanage.oms.audit.service.AuditService;
import com.orphanage.oms.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only audit log read endpoints.
 */
@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "List audit logs with pagination, search, and filters")
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AuditModule module,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return PageResponse.from(
                auditService.list(search, module, action, username, entityId, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log details by id")
    public AuditLogResponse getById(@PathVariable UUID id) {
        return auditService.getById(id);
    }
}
