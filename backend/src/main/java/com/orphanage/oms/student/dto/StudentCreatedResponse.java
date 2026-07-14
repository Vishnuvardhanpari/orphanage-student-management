package com.orphanage.oms.student.dto;

import com.orphanage.oms.student.enums.StudentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Minimal response returned after successful student registration.
 */
public record StudentCreatedResponse(
        UUID id,
        String admissionNumber,
        StudentStatus status,
        Instant createdDate
) {
}
