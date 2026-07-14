package com.orphanage.oms.student.controller;

import com.orphanage.oms.student.dto.CreateStudentRequest;
import com.orphanage.oms.student.dto.StudentCreatedResponse;
import com.orphanage.oms.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Student registration endpoints.
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
}
