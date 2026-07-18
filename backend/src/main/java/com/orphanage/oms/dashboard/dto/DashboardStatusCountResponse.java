package com.orphanage.oms.dashboard.dto;

import com.orphanage.oms.student.enums.StudentStatus;

/**
 * Count of students for a lifecycle status (includes soft-deleted).
 */
public record DashboardStatusCountResponse(StudentStatus status, long count) {
}
