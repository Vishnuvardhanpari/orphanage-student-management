package com.orphanage.oms.student.dto;

import com.orphanage.oms.student.enums.DocumentType;
import java.time.Instant;
import java.util.UUID;

/**
 * Document metadata for profile listing. Does not expose storage paths.
 */
public record StudentDocumentResponse(
        UUID id,
        DocumentType documentType,
        String originalFileName,
        String contentType,
        long fileSize,
        Instant uploadedDate
) {
}
