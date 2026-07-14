package com.orphanage.oms.student.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orphanage.oms.exception.ApiException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class StudentFileValidatorTest {

    private static final byte[] JPEG = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
    private static final byte[] PNG = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
    };
    private static final byte[] PDF = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);

    private StudentFileValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StudentFileValidator();
    }

    @Test
    void acceptsValidPhoto() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "face.png", "image/png", PNG);
        validator.validatePhoto(photo);
    }

    @Test
    void acceptsValidJpegPhoto() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "face.jpg", "image/jpeg", JPEG);
        validator.validatePhoto(photo);
    }

    @Test
    void rejectsSpoofedJpegContent() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "face.jpg", "image/jpeg", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> validator.validatePhoto(photo))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("content does not match");
    }

    @Test
    void rejectsSpoofedPdfDocument() {
        MockMultipartFile document = new MockMultipartFile(
                "documents", "aadhaar.pdf", "application/pdf", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> validator.validateDocument(document))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("content does not match");
    }

    @Test
    void acceptsValidPdfDocument() {
        MockMultipartFile document = new MockMultipartFile(
                "documents", "aadhaar.pdf", "application/pdf", PDF);
        validator.validateDocument(document);
    }

    @Test
    void rejectsOversizedDocument() {
        byte[] large = new byte[(int) StudentFileValidator.MAX_FILE_SIZE_BYTES + 1];
        System.arraycopy(PDF, 0, large, 0, PDF.length);
        MockMultipartFile document = new MockMultipartFile(
                "documents", "big.pdf", "application/pdf", large);

        assertThatThrownBy(() -> validator.validateDocument(document))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    void rejectsExecutableExtension() {
        MockMultipartFile document = new MockMultipartFile(
                "documents", "malware.exe", "application/pdf", PDF);

        assertThatThrownBy(() -> validator.validateDocument(document))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Executable");
    }

    @Test
    void rejectsPdfAsPhoto() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "face.pdf", "application/pdf", PDF);

        assertThatThrownBy(() -> validator.validatePhoto(photo))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("JPG");
    }
}
