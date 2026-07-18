package com.orphanage.oms.report.controller;

import com.orphanage.oms.report.dto.GeneratedReport;
import com.orphanage.oms.report.dto.ReportFilterRequest;
import com.orphanage.oms.report.dto.ReportStudentsRequest;
import com.orphanage.oms.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PDF report export endpoints.
 */
@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Generate a PDF report for a single student")
    public ResponseEntity<byte[]> exportSingleStudent(@PathVariable UUID studentId) {
        return toPdfResponse(reportService.exportSingleStudent(studentId));
    }

    @PostMapping("/students")
    @Operation(summary = "Generate a PDF report for selected students")
    public ResponseEntity<byte[]> exportSelectedStudents(
            @Valid @RequestBody ReportStudentsRequest request) {
        return toPdfResponse(reportService.exportSelectedStudents(request.studentIds()));
    }

    @PostMapping("/filter")
    @Operation(summary = "Generate a PDF report for students matching filters")
    public ResponseEntity<byte[]> exportFilteredStudents(
            @RequestBody(required = false) ReportFilterRequest request) {
        return toPdfResponse(reportService.exportFilteredStudents(request));
    }

    private static ResponseEntity<byte[]> toPdfResponse(GeneratedReport report) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(report.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(report.content().length)
                .body(report.content());
    }
}
