package com.orphanage.oms.exception;

import com.orphanage.oms.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler returning consistent error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles bean validation errors from request body validation.
     *
     * @param ex      the validation exception
     * @param request the current HTTP request
     * @return a structured validation error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Error", message, request.getRequestURI());
    }

    /**
     * Handles constraint violation errors from parameter validation.
     *
     * @param ex      the constraint violation exception
     * @param request the current HTTP request
     * @return a structured validation error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Error", message, request.getRequestURI());
    }

    /**
     * Handles domain API exceptions.
     *
     * @param ex      the API exception
     * @param request the current HTTP request
     * @return a structured error response
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request) {

        return buildResponse(ex.getStatus(), ex.getError(), ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handles Spring Security authentication failures raised in controllers/services.
     *
     * @param ex      the authentication exception
     * @param request the current HTTP request
     * @return a structured unauthorized response
     */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Invalid username or password.",
                request.getRequestURI());
    }

    /**
     * Handles access denied exceptions.
     *
     * @param ex      the access denied exception
     * @param request the current HTTP request
     * @return a structured forbidden response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "You do not have permission to access this resource.",
                request.getRequestURI());
    }

    /**
     * Handles requests that do not match any controller or static resource.
     *
     * @param ex      the missing-resource exception
     * @param request the current HTTP request
     * @return a structured not-found error response
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        log.warn("Resource not found at {}: {}", request.getRequestURI(), ex.getResourcePath());

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Not Found",
                "The requested resource was not found.",
                request.getRequestURI());
    }

    /**
     * Handles all unhandled exceptions.
     *
     * @param ex      the exception
     * @param request the current HTTP request
     * @return a generic internal server error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred.",
                request.getRequestURI());
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message,
            String path) {

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                path);

        return ResponseEntity.status(status).body(body);
    }
}
