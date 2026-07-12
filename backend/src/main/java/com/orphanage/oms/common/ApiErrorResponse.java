package com.orphanage.oms.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard API error response envelope.
 *
 * @param timestamp when the error occurred
 * @param status    HTTP status code
 * @param error     short error category
 * @param message   human-readable error message
 * @param path      request path that caused the error
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
