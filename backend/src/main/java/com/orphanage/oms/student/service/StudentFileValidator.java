package com.orphanage.oms.student.service;

import com.orphanage.oms.exception.ApiException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploaded student photo and document files.
 */
@Component
public class StudentFileValidator {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );
    private static final Set<String> DOCUMENT_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "application/pdf"
    );

    private static final byte[] JPEG_SIGNATURE = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] PDF_SIGNATURE = new byte[] {0x25, 0x50, 0x44, 0x46}; // %PDF

    public void validatePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            return;
        }
        validateCommon(photo, "photo");
        String extension = extensionOf(photo.getOriginalFilename());
        if (!IMAGE_EXTENSIONS.contains(extension)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Photo must be JPG, JPEG, or PNG.");
        }
        String contentType = normalizeContentType(photo.getContentType());
        if (!IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Photo content type must be image/jpeg or image/png.");
        }
        validateMagicBytes(photo, extension, contentType, "photo");
    }

    public void validateDocument(MultipartFile document) {
        if (document == null || document.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Document file must not be empty.");
        }
        validateCommon(document, "document");
        String extension = extensionOf(document.getOriginalFilename());
        if (!DOCUMENT_EXTENSIONS.contains(extension)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Documents must be PDF, JPG, JPEG, or PNG.");
        }
        String contentType = normalizeContentType(document.getContentType());
        if (!DOCUMENT_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Document content type must be application/pdf, image/jpeg, or image/png.");
        }
        validateMagicBytes(document, extension, contentType, "document");
    }

    public String extensionOf(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Uploaded file must have a filename with an extension.");
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Uploaded file must have a filename with an extension.");
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void validateCommon(MultipartFile file, String label) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1)
                            + " must not exceed 10 MB.");
        }
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".exe")
                    || lower.endsWith(".bat")
                    || lower.endsWith(".cmd")
                    || lower.endsWith(".sh")
                    || lower.endsWith(".js")
                    || lower.endsWith(".jar")) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Validation Error",
                        "Executable or script uploads are not allowed.");
            }
        }
    }

    private void validateMagicBytes(
            MultipartFile file,
            String extension,
            String contentType,
            String label) {
        byte[] header = readHeader(file, 8);
        DetectedContent detected = detectContent(header);

        boolean extensionMatches = switch (extension) {
            case "jpg", "jpeg" -> detected == DetectedContent.JPEG;
            case "png" -> detected == DetectedContent.PNG;
            case "pdf" -> detected == DetectedContent.PDF;
            default -> false;
        };
        boolean mimeMatches = switch (contentType) {
            case "image/jpeg" -> detected == DetectedContent.JPEG;
            case "image/png" -> detected == DetectedContent.PNG;
            case "application/pdf" -> detected == DetectedContent.PDF;
            default -> false;
        };

        if (!extensionMatches || !mimeMatches) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1)
                            + " content does not match the declared file type.");
        }
    }

    private static byte[] readHeader(MultipartFile file, int length) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Validation Error",
                        "Uploaded file must not be empty.");
            }
            return Arrays.copyOf(bytes, Math.min(bytes.length, length));
        } catch (IOException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Unable to read uploaded file content.");
        }
    }

    private static DetectedContent detectContent(byte[] header) {
        if (startsWith(header, JPEG_SIGNATURE)) {
            return DetectedContent.JPEG;
        }
        if (startsWith(header, PNG_SIGNATURE)) {
            return DetectedContent.PNG;
        }
        if (startsWith(header, PDF_SIGNATURE)) {
            return DetectedContent.PDF;
        }
        return DetectedContent.UNKNOWN;
    }

    private static boolean startsWith(byte[] header, byte[] signature) {
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String normalized = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private enum DetectedContent {
        JPEG,
        PNG,
        PDF,
        UNKNOWN
    }
}
