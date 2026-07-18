package com.orphanage.oms.dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Compact student row for dashboard recent lists.
 */
public record DashboardRecentStudentResponse(
        UUID id,
        String firstName,
        String lastName,
        String admissionNumber,
        LocalDate admissionDate,
        Instant updatedDate
) {
}
