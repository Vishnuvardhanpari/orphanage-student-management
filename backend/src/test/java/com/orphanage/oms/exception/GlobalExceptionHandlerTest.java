package com.orphanage.oms.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.orphanage.oms.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/students");
    }

    @Test
    void mapsAdmissionUniqueConstraintToConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "constraint",
                new RuntimeException("duplicate key value violates unique constraint \"uq_students_admission_number_lower\""));

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Admission number");
        assertThat(response.getBody().error()).isEqualTo("Conflict");
    }

    @Test
    void mapsAadhaarUniqueConstraintToConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "constraint",
                new RuntimeException("Detail: Key (aadhaar_number)=(123456789012) already exists."));

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Aadhaar number");
    }

    @Test
    void mapsMaxUploadSizeExceededToSizeLimitMessage() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10L * 1024 * 1024);

        ResponseEntity<ApiErrorResponse> response = handler.handleMaxUploadSizeExceeded(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Validation Error");
        assertThat(response.getBody().message()).contains("10 MB");
        assertThat(response.getBody().message()).contains("50 MB");
    }

    @Test
    void mapsGenericMultipartExceptionToInvalidMultipartMessage() {
        MultipartException ex = new MultipartException("Failed to parse multipart servlet request");

        ResponseEntity<ApiErrorResponse> response = handler.handleMultipartException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Validation Error");
        assertThat(response.getBody().message()).isEqualTo("Invalid multipart request.");
        assertThat(response.getBody().message()).doesNotContain("10 MB");
    }

    @Test
    void mapsMultipartExceptionWithSizeCauseToSizeLimitMessage() {
        MultipartException ex = new MultipartException(
                "Current request is exceeded",
                new RuntimeException("the request was rejected because its size exceeds the configured maximum"));

        ResponseEntity<ApiErrorResponse> response = handler.handleMultipartException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("10 MB");
        assertThat(response.getBody().message()).contains("50 MB");
    }
}
