package com.orphanage.oms.audit.enums;

/**
 * Business action recorded in an audit event.
 */
public enum AuditAction {
    LOGIN,
    LOGOUT,
    CREATED,
    UPDATED,
    DELETED,
    RESTORED,
    UPLOADED,
    REPLACED,
    GENERATED
}
