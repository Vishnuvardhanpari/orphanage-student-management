package com.orphanage.oms.student.dto;

import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Compact student row for the paginated list/search API (Milestone 8).
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
        LocalDate admissionDate
) {
}
