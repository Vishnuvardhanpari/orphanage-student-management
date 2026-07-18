package com.orphanage.oms.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.orphanage.oms.report.config.ReportProperties;
import com.orphanage.oms.storage.StorageService;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.entity.StudentDocument;
import com.orphanage.oms.student.enums.DocumentType;
import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentPdfRendererTest {

    private static final byte[] PNG_1X1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Mock
    private StorageService storageService;

    private StudentPdfRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new StudentPdfRenderer(
                new ReportProperties("Test Orphanage", 50, 100), storageService);
    }

    @Test
    void rendersPdfWithPhotoImageDocAndPdfReference() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder()
                .id(studentId)
                .admissionNumber("ADM-PDF-1")
                .firstName("Anita")
                .lastName("Sharma")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(2014, 3, 15))
                .admissionDate(LocalDate.of(2024, 6, 1))
                .status(StudentStatus.ACTIVE)
                .profilePhotoPath("photos/anita.png")
                .schoolName("Demo School")
                .build();

        StudentDocument imageDoc = StudentDocument.builder()
                .id(UUID.randomUUID())
                .student(student)
                .documentType(DocumentType.MARK_SHEET)
                .originalFileName("mark.png")
                .storedFileName("mark.png")
                .storagePath("docs/mark.png")
                .contentType("image/png")
                .fileSize(PNG_1X1.length)
                .uploadedDate(Instant.parse("2024-06-02T10:00:00Z"))
                .build();

        StudentDocument pdfDoc = StudentDocument.builder()
                .id(UUID.randomUUID())
                .student(student)
                .documentType(DocumentType.AADHAAR_CARD)
                .originalFileName("aadhaar.pdf")
                .storedFileName("aadhaar.pdf")
                .storagePath("docs/aadhaar.pdf")
                .contentType("application/pdf")
                .fileSize(20)
                .uploadedDate(Instant.parse("2024-06-02T11:00:00Z"))
                .build();

        when(storageService.load(anyString()))
                .thenAnswer(invocation -> new ByteArrayInputStream(PNG_1X1));

        byte[] pdf = renderer.render(
                List.of(student),
                Map.of(studentId, List.of(imageDoc, pdfDoc)),
                "admin",
                Instant.parse("2024-07-01T12:00:00Z"));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        // Content streams are Flate-compressed; assert successful generation rather than plaintext.
        assertThat(pdf.length).isGreaterThan(1_000);
    }

    @Test
    void continuesWhenPhotoMissing() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder()
                .id(studentId)
                .admissionNumber("ADM-PDF-2")
                .firstName("Ravi")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(2015, 1, 1))
                .admissionDate(LocalDate.of(2024, 1, 1))
                .status(StudentStatus.ACTIVE)
                .build();

        byte[] pdf = renderer.render(
                List.of(student),
                Map.of(studentId, List.of()),
                "staff",
                Instant.now());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }
}
