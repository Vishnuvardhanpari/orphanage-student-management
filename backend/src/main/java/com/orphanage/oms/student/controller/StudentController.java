package com.orphanage.oms.student.controller;

import com.orphanage.oms.student.dto.CreateStudentRequest;
import com.orphanage.oms.student.dto.StudentCreatedResponse;
import com.orphanage.oms.student.dto.StudentDetailResponse;
import com.orphanage.oms.student.dto.StudentDocumentResponse;
import com.orphanage.oms.student.dto.StoredFilePayload;
import com.orphanage.oms.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Student registration and profile endpoints.
 */
@RestController
@RequestMapping("/api/v1/students")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Students")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a student with optional photo and supporting documents")
    public StudentCreatedResponse create(
            @Valid @RequestPart("data") CreateStudentRequest data,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents,
            @RequestParam(value = "documentTypes", required = false) List<String> documentTypes) {
        return studentService.create(data, photo, documents, documentTypes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get student profile by id")
    public StudentDetailResponse getById(@PathVariable UUID id) {
        return studentService.getById(id);
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "List active supporting documents for a student")
    public List<StudentDocumentResponse> listDocuments(@PathVariable UUID id) {
        return studentService.listDocuments(id);
    }

    @GetMapping("/{id}/documents/{documentId}/download")
    @Operation(summary = "Download a supporting document")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable UUID id, @PathVariable UUID documentId) {
        StoredFilePayload file = studentService.downloadDocument(id, documentId);
        return toFileResponse(file, ContentDisposition.attachment());
    }

    @GetMapping("/{id}/photo")
    @Operation(summary = "Stream the student profile photo")
    public ResponseEntity<InputStreamResource> getPhoto(@PathVariable UUID id) {
        StoredFilePayload file = studentService.loadProfilePhoto(id);
        return toFileResponse(file, ContentDisposition.inline());
    }

    private static ResponseEntity<InputStreamResource> toFileResponse(
            StoredFilePayload file, ContentDisposition.Builder dispositionBuilder) {
        ContentDisposition disposition = dispositionBuilder
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType()));
        if (file.contentLength() >= 0) {
            builder.contentLength(file.contentLength());
        }
        return builder.body(new InputStreamResource(file.content()));
    }
}
