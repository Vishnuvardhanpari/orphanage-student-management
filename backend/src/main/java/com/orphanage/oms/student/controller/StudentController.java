package com.orphanage.oms.student.controller;

import com.orphanage.oms.common.dto.PageResponse;
import com.orphanage.oms.student.dto.CreateStudentRequest;
import com.orphanage.oms.student.dto.StudentCreatedResponse;
import com.orphanage.oms.student.dto.StudentDetailResponse;
import com.orphanage.oms.student.dto.SoftDeleteStudentRequest;
import com.orphanage.oms.student.dto.StudentDocumentResponse;
import com.orphanage.oms.student.dto.StudentSummaryResponse;
import com.orphanage.oms.student.dto.StoredFilePayload;
import com.orphanage.oms.student.dto.UpdateStudentRequest;
import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import com.orphanage.oms.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Student registration, profile, update, soft-delete, and restore endpoints.
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

    @GetMapping
    @Operation(summary = "List students with pagination, sorting, global search, and filters")
    public PageResponse<StudentSummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String admissionNumber,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) Integer admissionYear,
            @RequestParam(required = false) String school,
            @RequestParam(required = false) Integer ageMin,
            @RequestParam(required = false) Integer ageMax,
            @PageableDefault(size = 20, sort = "admissionDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.from(studentService.list(
                search,
                admissionNumber,
                gender,
                status,
                admissionYear,
                school,
                ageMin,
                ageMax,
                pageable));
    }

    @GetMapping("/inactive")
    @Operation(summary = "List soft-deleted (archived) students with optional filters")
    public PageResponse<StudentSummaryResponse> listInactive(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) Integer admissionYear,
            @RequestParam(required = false) String school,
            @RequestParam(required = false) Integer ageMin,
            @RequestParam(required = false) Integer ageMax,
            @PageableDefault(size = 20, sort = "deletedDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.from(studentService.listInactive(
                search, gender, admissionYear, school, ageMin, ageMax, pageable));
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
    @Operation(summary = "Get student profile by id (includes archived)")
    public StudentDetailResponse getById(@PathVariable UUID id) {
        return studentService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update student personal, guardian, education, and medical fields")
    public StudentDetailResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateStudentRequest request) {
        return studentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete (archive) a student, optionally recording exit details")
    public void softDelete(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) SoftDeleteStudentRequest exitDetails) {
        studentService.softDelete(id, exitDetails);
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Restore a soft-deleted student (ADMIN only)")
    public StudentDetailResponse restore(@PathVariable UUID id) {
        return studentService.restore(id);
    }

    @PutMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Replace the student profile photo")
    public void replacePhoto(
            @PathVariable UUID id, @RequestPart("photo") MultipartFile photo) {
        studentService.replacePhoto(id, photo);
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "List active supporting documents for a student")
    public List<StudentDocumentResponse> listDocuments(@PathVariable UUID id) {
        return studentService.listDocuments(id);
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload additional supporting documents")
    public List<StudentDocumentResponse> addDocuments(
            @PathVariable UUID id,
            @RequestPart("documents") List<MultipartFile> documents,
            @RequestParam("documentTypes") List<String> documentTypes) {
        return studentService.addDocuments(id, documents, documentTypes);
    }

    @PutMapping(
            value = "/{id}/documents/{documentId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Replace an existing supporting document")
    public StudentDocumentResponse replaceDocument(
            @PathVariable UUID id,
            @PathVariable UUID documentId,
            @RequestPart("document") MultipartFile document,
            @RequestParam(value = "documentType", required = false) String documentType) {
        return studentService.replaceDocument(id, documentId, document, documentType);
    }

    @DeleteMapping("/{id}/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove the student profile photo")
    public void deletePhoto(@PathVariable UUID id) {
        studentService.deletePhoto(id);
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logically remove a supporting document")
    public void deleteDocument(@PathVariable UUID id, @PathVariable UUID documentId) {
        studentService.deleteDocument(id, documentId);
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
