package com.orphanage.oms.student.dto;

import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Compact student row for the paginated list/search API (Milestone 8).
 *
 * <p>{@code deletedDate} is only populated for archived rows returned by
 * {@code GET /students/inactive} (Milestone 9 QA — BUG-007); it is always
 * {@code null} for active students returned by {@code GET /students}.
 */
public record StudentSummaryResponse(
        UUID id,
        String admissionNumber,
        String firstName,
        String lastName,
        Gender gender,
        LocalDate dateOfBirth,
        StudentStatus status,
        String schoolName,
        String standard,
        LocalDate admissionDate,
        Instant deletedDate
) {
}
