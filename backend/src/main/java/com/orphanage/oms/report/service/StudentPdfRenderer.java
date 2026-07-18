package com.orphanage.oms.report.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.orphanage.oms.report.config.ReportProperties;
import com.orphanage.oms.storage.StorageService;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.entity.StudentDocument;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Renders student PDF reports with OpenPDF.
 *
 * <p>Image documents (JPG/JPEG/PNG) are embedded inline. PDF documents appear as
 * reference rows only (type, file name, upload date).
 */
@Component
public class StudentPdfRenderer {

    private static final Logger log = LoggerFactory.getLogger(StudentPdfRenderer.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(30, 64, 175));
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.GRAY);
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
    private static final Font META_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

    private final ReportProperties reportProperties;
    private final StorageService storageService;

    public StudentPdfRenderer(ReportProperties reportProperties, StorageService storageService) {
        this.reportProperties = reportProperties;
        this.storageService = storageService;
    }

    /**
     * Builds a multi-student PDF. Each student starts on a new page after the first.
     *
     * @param students       ordered students to render
     * @param documentsByStudent documents keyed by student id (active docs only)
     * @param generatedBy    username of the requesting user
     * @param generatedAt    generation timestamp
     * @return PDF bytes
     */
    public byte[] render(
            List<Student> students,
            java.util.Map<java.util.UUID, List<StudentDocument>> documentsByStudent,
            String generatedBy,
            Instant generatedAt) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 48, 48, 56, 56);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PageNumberFooter(reportProperties.organizationName()));
            document.open();

            boolean first = true;
            for (Student student : students) {
                if (!first) {
                    document.newPage();
                }
                first = false;
                List<StudentDocument> docs =
                        documentsByStudent.getOrDefault(student.getId(), List.of());
                renderStudent(document, student, docs, generatedBy, generatedAt);
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Failed to generate PDF report.", ex);
        }
    }

    private void renderStudent(
            Document document,
            Student student,
            List<StudentDocument> documents,
            String generatedBy,
            Instant generatedAt)
            throws DocumentException, IOException {
        Paragraph org = new Paragraph(nullToEmpty(reportProperties.organizationName()), TITLE_FONT);
        org.setAlignment(Element.ALIGN_CENTER);
        org.setSpacingAfter(4);
        document.add(org);

        Paragraph title = new Paragraph("Student Report", HEADER_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(8);
        document.add(title);

        Paragraph meta = new Paragraph();
        meta.setAlignment(Element.ALIGN_CENTER);
        meta.setFont(META_FONT);
        meta.add("Generated: " + DATETIME_FMT.format(generatedAt));
        meta.add("  ·  Generated by: " + nullToDash(generatedBy));
        meta.setSpacingAfter(16);
        document.add(meta);

        PdfPTable headerRow = new PdfPTable(new float[] {1.2f, 2.8f});
        headerRow.setWidthPercentage(100);
        headerRow.setSpacingAfter(12);

        PdfPCell photoCell = new PdfPCell();
        photoCell.setBorder(Rectangle.NO_BORDER);
        photoCell.setPadding(4);
        Image photo = loadProfilePhoto(student);
        if (photo != null) {
            photo.scaleToFit(110, 130);
            photoCell.addElement(photo);
        } else {
            photoCell.addElement(new Paragraph("No photo available", VALUE_FONT));
        }
        headerRow.addCell(photoCell);

        PdfPCell identityCell = new PdfPCell();
        identityCell.setBorder(Rectangle.NO_BORDER);
        identityCell.setPadding(4);
        identityCell.addElement(new Paragraph(
                fullName(student), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK)));
        identityCell.addElement(labeledLine("Admission No.", student.getAdmissionNumber()));
        identityCell.addElement(labeledLine("Status", enumLabel(student.getStatus())));
        identityCell.addElement(labeledLine("Gender", enumLabel(student.getGender())));
        identityCell.addElement(labeledLine(
                "Date of Birth",
                student.getDateOfBirth() != null ? DATE_FMT.format(student.getDateOfBirth()) : "—"));
        headerRow.addCell(identityCell);
        document.add(headerRow);

        addSection(document, "Personal Information");
        addKeyValueTable(document, List.of(
                entry("Blood Group", student.getBloodGroup()),
                entry("Religion", student.getReligion()),
                entry("Nationality", student.getNationality()),
                entry("Aadhaar Number", student.getAadhaarNumber()),
                entry("Phone Number", student.getPhoneNumber())));

        addSection(document, "Guardian Information");
        addKeyValueTable(document, List.of(
                entry("Guardian Name", student.getGuardianName()),
                entry("Relationship", student.getGuardianRelationship()),
                entry("Guardian Phone", student.getGuardianPhone()),
                entry("Guardian Address", student.getGuardianAddress())));

        addSection(document, "Education");
        addKeyValueTable(document, List.of(
                entry("School", student.getSchoolName()),
                entry("Standard", student.getStandard()),
                entry("Medium", student.getMedium()),
                entry("Previous School", student.getPreviousSchool())));

        addSection(document, "Medical Information");
        addKeyValueTable(document, List.of(
                entry("Medical Conditions", student.getMedicalConditions()),
                entry("Allergies", student.getAllergies()),
                entry("Disability", student.getDisability()),
                entry("Emergency Notes", student.getEmergencyNotes())));

        addSection(document, "Admission / Exit");
        List<String[]> admissionRows = new ArrayList<>();
        admissionRows.add(entry(
                "Admission Date",
                student.getAdmissionDate() != null ? DATE_FMT.format(student.getAdmissionDate()) : null));
        if (student.getExitDate() != null) {
            admissionRows.add(entry("Exit Date", DATE_FMT.format(student.getExitDate())));
            admissionRows.add(entry("Exit Reason", student.getExitReason()));
            admissionRows.add(entry("Exit Remarks", student.getExitRemarks()));
        }
        addKeyValueTable(document, admissionRows);

        List<StudentDocument> imageDocs = new ArrayList<>();
        List<StudentDocument> pdfDocs = new ArrayList<>();
        for (StudentDocument doc : documents) {
            if (isImageContentType(doc.getContentType())) {
                imageDocs.add(doc);
            } else {
                pdfDocs.add(doc);
            }
        }

        addSection(document, "Supporting Documents (Images)");
        if (imageDocs.isEmpty()) {
            document.add(new Paragraph("No image documents on file.", VALUE_FONT));
        } else {
            for (StudentDocument doc : imageDocs) {
                Paragraph caption = new Paragraph(
                        enumLabel(doc.getDocumentType())
                                + " — "
                                + nullToDash(doc.getOriginalFileName())
                                + " ("
                                + DATETIME_FMT.format(doc.getUploadedDate())
                                + ")",
                        LABEL_FONT);
                caption.setSpacingBefore(8);
                caption.setSpacingAfter(4);
                document.add(caption);

                Image embedded = loadDocumentImage(doc);
                if (embedded != null) {
                    embedded.scaleToFit(420, 320);
                    embedded.setAlignment(Element.ALIGN_LEFT);
                    document.add(embedded);
                } else {
                    document.add(new Paragraph("Image unavailable.", VALUE_FONT));
                }
            }
        }

        addSection(document, "Supporting Documents (PDF References)");
        if (pdfDocs.isEmpty()) {
            document.add(new Paragraph("No PDF documents on file.", VALUE_FONT));
        } else {
            PdfPTable table = new PdfPTable(new float[] {2f, 3f, 2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(6);
            addHeaderCell(table, "Type");
            addHeaderCell(table, "File Name");
            addHeaderCell(table, "Uploaded");
            for (StudentDocument doc : pdfDocs) {
                addBodyCell(table, enumLabel(doc.getDocumentType()));
                addBodyCell(table, nullToDash(doc.getOriginalFileName()));
                addBodyCell(table, DATETIME_FMT.format(doc.getUploadedDate()));
            }
            document.add(table);
        }
    }

    private Image loadProfilePhoto(Student student) {
        String path = student.getProfilePhotoPath();
        if (path == null || path.isBlank()) {
            return null;
        }
        return loadImageFromStorage(path, "profile photo for student " + student.getId());
    }

    private Image loadDocumentImage(StudentDocument document) {
        return loadImageFromStorage(
                document.getStoragePath(), "document " + document.getId());
    }

    private Image loadImageFromStorage(String relativePath, String context) {
        try (InputStream in = storageService.load(relativePath)) {
            byte[] bytes = in.readAllBytes();
            return Image.getInstance(bytes);
        } catch (Exception ex) {
            log.warn("Unable to load image for {}: {}", context, ex.getMessage());
            return null;
        }
    }

    private static boolean isImageContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        return lower.startsWith("image/jpeg")
                || lower.startsWith("image/jpg")
                || lower.startsWith("image/png");
    }

    private static void addSection(Document document, String title) throws DocumentException {
        Paragraph section = new Paragraph(title, SECTION_FONT);
        section.setSpacingBefore(14);
        section.setSpacingAfter(6);
        document.add(section);
        document.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(
                0.5f, 100, new Color(200, 200, 200), Element.ALIGN_LEFT, -2)));
    }

    private static void addKeyValueTable(Document document, List<String[]> rows)
            throws DocumentException {
        PdfPTable table = new PdfPTable(new float[] {1.4f, 3.6f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        for (String[] row : rows) {
            PdfPCell label = new PdfPCell(new Phrase(row[0], LABEL_FONT));
            label.setBorder(Rectangle.NO_BORDER);
            label.setPadding(3);
            PdfPCell value = new PdfPCell(new Phrase(nullToDash(row[1]), VALUE_FONT));
            value.setBorder(Rectangle.NO_BORDER);
            value.setPadding(3);
            table.addCell(label);
            table.addCell(value);
        }
        document.add(table);
    }

    private static void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, LABEL_FONT));
        cell.setBackgroundColor(new Color(243, 244, 246));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private static void addBodyCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, VALUE_FONT));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private static Paragraph labeledLine(String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", LABEL_FONT));
        p.add(new Chunk(nullToDash(value), VALUE_FONT));
        p.setSpacingAfter(2);
        return p;
    }

    private static String[] entry(String label, String value) {
        return new String[] {label, value};
    }

    private static String fullName(Student student) {
        String last = student.getLastName();
        if (last == null || last.isBlank()) {
            return student.getFirstName();
        }
        return student.getFirstName() + " " + last;
    }

    private static String enumLabel(Enum<?> value) {
        if (value == null) {
            return "—";
        }
        return value.name().replace('_', ' ');
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String nullToDash(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value;
    }

    private static final class PageNumberFooter extends PdfPageEventHelper {

        private final String organizationName;

        private PageNumberFooter(String organizationName) {
            this.organizationName = organizationName == null ? "" : organizationName;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(2);
            try {
                footer.setWidths(new int[] {7, 3});
                footer.setTotalWidth(
                        document.getPageSize().getWidth()
                                - document.leftMargin()
                                - document.rightMargin());
                footer.getDefaultCell().setBorder(Rectangle.NO_BORDER);
                footer.getDefaultCell().setPadding(0);

                PdfPCell left = new PdfPCell(new Phrase(organizationName, FOOTER_FONT));
                left.setBorder(Rectangle.TOP);
                left.setBorderColor(new Color(200, 200, 200));
                left.setPaddingTop(6);
                footer.addCell(left);

                PdfPCell right = new PdfPCell(
                        new Phrase("Page " + writer.getPageNumber(), FOOTER_FONT));
                right.setHorizontalAlignment(Element.ALIGN_RIGHT);
                right.setBorder(Rectangle.TOP);
                right.setBorderColor(new Color(200, 200, 200));
                right.setPaddingTop(6);
                footer.addCell(right);

                footer.writeSelectedRows(
                        0,
                        -1,
                        document.leftMargin(),
                        document.bottomMargin() - 10,
                        writer.getDirectContent());
            } catch (DocumentException ignored) {
                // Footer failure must not abort PDF generation.
            }
        }
    }
}
