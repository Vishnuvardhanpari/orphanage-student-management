package com.orphanage.oms.audit.mapper;

import com.orphanage.oms.audit.dto.AuditLogResponse;
import com.orphanage.oms.audit.entity.AuditLog;
import org.mapstruct.Mapper;

/**
 * Maps audit entities to API DTOs.
 */
@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog entity);
}
