package com.orphanage.oms.student.dto;

import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Optional exit details captured when archiving a student (Milestone 9 QA —
 * BUG-005). The request body itself is optional on {@code DELETE
 * /students/{id}}; omitting it (or any individual field) preserves the
 * original flags-only soft delete behavior.
 */
public record SoftDeleteStudentRequest(
        @PastOrPresent LocalDate exitDate,
        String exitReason,
        String exitRemarks
) {
}
