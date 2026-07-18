package com.orphanage.oms.report.service;

import com.orphanage.oms.exception.ApiException;
import com.orphanage.oms.report.config.ReportProperties;
import com.orphanage.oms.report.dto.GeneratedReport;
import com.orphanage.oms.report.dto.ReportFilterRequest;
import com.orphanage.oms.report.dto.ReportStudentScope;
import com.orphanage.oms.security.UserPrincipal;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.entity.StudentDocument;
import com.orphanage.oms.student.repository.StudentDeletedQuery;
import com.orphanage.oms.student.repository.StudentDocumentRepository;
import com.orphanage.oms.student.repository.StudentRepository;
import com.orphanage.oms.student.repository.StudentSpecifications;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves students and documents, then generates PDF reports.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").withZone(ZoneOffset.UTC);

    private static final Sort ADMISSION_DATE_DESC = Sort.by(Sort.Direction.DESC, "admissionDate");

    private final StudentRepository studentRepository;
    private final StudentDeletedQuery studentDeletedQuery;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentPdfRenderer studentPdfRenderer;
    private final ReportProperties reportProperties;

    public ReportService(
            StudentRepository studentRepository,
            StudentDeletedQuery studentDeletedQuery,
            StudentDocumentRepository studentDocumentRepository,
            StudentPdfRenderer studentPdfRenderer,
            ReportProperties reportProperties) {
        this.studentRepository = studentRepository;
        this.studentDeletedQuery = studentDeletedQuery;
        this.studentDocumentRepository = studentDocumentRepository;
        this.studentPdfRenderer = studentPdfRenderer;
        this.reportProperties = reportProperties;
    }

    @Transactional(readOnly = true)
    public GeneratedReport exportSingleStudent(UUID studentId) {
        Student student = studentRepository
                .findIncludingDeletedById(studentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Not Found", "Student not found."));

        String generatedBy = currentUsername();
        Instant generatedAt = Instant.now();
        byte[] pdf = renderStudents(List.of(student), generatedBy, generatedAt);

        log.info(
                "Report generated type=SINGLE studentCount=1 studentId={} generatedBy={}",
                studentId,
                generatedBy);

        String fileName = "student-report-" + sanitizeFilePart(student.getAdmissionNumber()) + ".pdf";
        return new GeneratedReport(pdf, fileName);
    }

    @Transactional(readOnly = true)
    public GeneratedReport exportSelectedStudents(List<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "At least one student id is required.");
        }
        if (studentIds.size() > reportProperties.maxSelectedStudents()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "At most " + reportProperties.maxSelectedStudents()
                            + " students may be selected for export.");
        }

        Set<UUID> uniqueIds = new HashSet<>(studentIds);
        List<Student> students = new ArrayList<>();
        for (UUID id : uniqueIds) {
            Student student = studentRepository
                    .findIncludingDeletedById(id)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "Not Found",
                            "Student not found: " + id));
            students.add(student);
        }
        students.sort((a, b) -> b.getAdmissionDate().compareTo(a.getAdmissionDate()));

        String generatedBy = currentUsername();
        Instant generatedAt = Instant.now();
        byte[] pdf = renderStudents(students, generatedBy, generatedAt);

        log.info(
                "Report generated type=SELECTED studentCount={} generatedBy={}",
                students.size(),
                generatedBy);

        return new GeneratedReport(pdf, bulkFileName(generatedAt));
    }

    @Transactional(readOnly = true)
    public GeneratedReport exportFilteredStudents(ReportFilterRequest request) {
        ReportFilterRequest filters = request != null
                ? request
                : new ReportFilterRequest(null, null, null, null, null, null, null);

        ReportStudentScope scope =
                filters.scope() != null ? filters.scope() : ReportStudentScope.ACTIVE;

        StudentSpecifications.validateAgeRange(filters.ageMin(), filters.ageMax());

        long matchCount = countFilteredStudents(scope, filters);
        if (matchCount == 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "No students match the selected filters.");
        }
        if (matchCount > reportProperties.maxFilterResults()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Filter matched "
                            + matchCount
                            + " students; maximum allowed is "
                            + reportProperties.maxFilterResults()
                            + ". Narrow your filters and try again.");
        }

        List<Student> students = resolveFilteredStudents(scope, filters);

        String generatedBy = currentUsername();
        Instant generatedAt = Instant.now();
        byte[] pdf = renderStudents(students, generatedBy, generatedAt);

        log.info(
                "Report generated type=FILTER scope={} studentCount={} generatedBy={} filters={}",
                scope,
                students.size(),
                generatedBy,
                summarizeFilters(scope, filters));

        return new GeneratedReport(pdf, bulkFileName(generatedAt));
    }

    private long countFilteredStudents(ReportStudentScope scope, ReportFilterRequest filters) {
        return switch (scope) {
            case ACTIVE -> countActiveMatching(filters);
            case ARCHIVED -> countArchivedMatching(filters);
            case ALL -> countActiveMatching(filters) + countArchivedMatching(filters);
        };
    }

    private long countActiveMatching(ReportFilterRequest filters) {
        return studentRepository.count(activeSpecification(filters));
    }

    private long countArchivedMatching(ReportFilterRequest filters) {
        LocalDate[] dobBounds = dobBounds(filters);
        return studentDeletedQuery.countDeletedMatching(
                blankToNull(filters.search()),
                filters.gender(),
                filters.admissionYear(),
                blankToNull(filters.school()),
                dobBounds[0],
                dobBounds[1]);
    }

    private List<Student> resolveFilteredStudents(
            ReportStudentScope scope, ReportFilterRequest filters) {
        return switch (scope) {
            case ACTIVE -> findActiveMatching(filters);
            case ARCHIVED -> findArchivedMatching(filters);
            case ALL -> {
                List<Student> combined = new ArrayList<>();
                combined.addAll(findActiveMatching(filters));
                combined.addAll(findArchivedMatching(filters));
                combined.sort(Comparator.comparing(Student::getAdmissionDate).reversed());
                yield combined;
            }
        };
    }

    /**
     * Active (non-deleted) students — same predicate path as {@code GET /students}.
     */
    private List<Student> findActiveMatching(ReportFilterRequest filters) {
        return studentRepository.findAll(activeSpecification(filters), ADMISSION_DATE_DESC);
    }

    private Specification<Student> activeSpecification(ReportFilterRequest filters) {
        return StudentSpecifications.buildListSpecification(
                filters.search(),
                null,
                filters.gender(),
                null,
                filters.admissionYear(),
                filters.school(),
                filters.ageMin(),
                filters.ageMax());
    }

    /**
     * Soft-deleted students — same population as {@code GET /students/inactive}.
     */
    private List<Student> findArchivedMatching(ReportFilterRequest filters) {
        LocalDate[] dobBounds = dobBounds(filters);
        return studentDeletedQuery.findDeletedMatching(
                blankToNull(filters.search()),
                filters.gender(),
                filters.admissionYear(),
                blankToNull(filters.school()),
                dobBounds[0],
                dobBounds[1],
                ADMISSION_DATE_DESC);
    }

    private static LocalDate[] dobBounds(ReportFilterRequest filters) {
        if (filters.ageMin() == null && filters.ageMax() == null) {
            return new LocalDate[] {null, null};
        }
        return StudentSpecifications.ageToDateOfBirthBounds(filters.ageMin(), filters.ageMax());
    }

    private byte[] renderStudents(List<Student> students, String generatedBy, Instant generatedAt) {
        Map<UUID, List<StudentDocument>> documentsByStudent = new LinkedHashMap<>();
        for (Student student : students) {
            documentsByStudent.put(
                    student.getId(),
                    studentDocumentRepository.findByStudent_IdOrderByUploadedDateDesc(student.getId()));
        }
        return studentPdfRenderer.render(students, documentsByStudent, generatedBy, generatedAt);
    }

    private static String bulkFileName(Instant generatedAt) {
        return "students-report-" + FILE_TS.format(generatedAt) + ".pdf";
    }

    private static String sanitizeFilePart(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String summarizeFilters(ReportStudentScope scope, ReportFilterRequest filters) {
        Map<String, Object> map = new HashMap<>();
        map.put("scope", scope);
        if (filters.search() != null && !filters.search().isBlank()) {
            map.put("search", filters.search().trim());
        }
        if (filters.gender() != null) {
            map.put("gender", filters.gender());
        }
        if (filters.admissionYear() != null) {
            map.put("admissionYear", filters.admissionYear());
        }
        if (filters.school() != null && !filters.school().isBlank()) {
            map.put("school", filters.school().trim());
        }
        if (filters.ageMin() != null) {
            map.put("ageMin", filters.ageMin());
        }
        if (filters.ageMax() != null) {
            map.put("ageMax", filters.ageMax());
        }
        return map.toString();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication required.");
        }
        return principal.getUsername();
    }
}
