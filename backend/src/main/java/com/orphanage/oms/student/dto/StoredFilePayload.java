package com.orphanage.oms.student.dto;

import java.io.InputStream;

/**
 * Stream plus metadata for authenticated file download / photo endpoints.
 */
public record StoredFilePayload(
        InputStream content,
        String contentType,
        String fileName,
        long contentLength
) {
}
