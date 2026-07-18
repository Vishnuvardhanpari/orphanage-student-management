package com.orphanage.oms.report.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Request body for exporting a selected set of students as a PDF.
 *
 * <p>Maximum selection size is enforced in {@code ReportService} using
 * {@code oms.reports.max-selected-students} (not Bean Validation), so ops can
 * raise the limit without a code change.
 */
public record ReportStudentsRequest(
        @NotEmpty(message = "At least one student id is required.")
        List<@NotNull UUID> studentIds
) {
}
