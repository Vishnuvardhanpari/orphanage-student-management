package com.orphanage.oms.report.dto;

import com.orphanage.oms.student.enums.Gender;

/**
 * Filter criteria for bulk PDF export.
 *
 * <p>{@code scope} selects active, archived, or both populations. Other fields
 * mirror {@code GET /students} / {@code GET /students/inactive} filters (no pagination).
 */
public record ReportFilterRequest(
        ReportStudentScope scope,
        String search,
        Gender gender,
        Integer admissionYear,
        String school,
        Integer ageMin,
        Integer ageMax
) {
}
