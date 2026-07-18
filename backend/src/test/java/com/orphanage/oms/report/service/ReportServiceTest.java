package com.orphanage.oms.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orphanage.oms.exception.ApiException;
import com.orphanage.oms.report.config.ReportProperties;
import com.orphanage.oms.report.dto.GeneratedReport;
import com.orphanage.oms.report.dto.ReportFilterRequest;
import com.orphanage.oms.report.dto.ReportStudentScope;
import com.orphanage.oms.security.UserPrincipal;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import com.orphanage.oms.student.repository.StudentDeletedQuery;
import com.orphanage.oms.student.repository.StudentDocumentRepository;
import com.orphanage.oms.student.repository.StudentRepository;
import com.orphanage.oms.user.entity.Role;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentDeletedQuery studentDeletedQuery;

    @Mock
    private StudentDocumentRepository studentDocumentRepository;

    @Mock
    private StudentPdfRenderer studentPdfRenderer;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                studentRepository,
                studentDeletedQuery,
                studentDocumentRepository,
                studentPdfRenderer,
                new ReportProperties("OMS", 50, 100));
        authenticateAs("admin");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exportSingleStudentReturnsPdf() {
        UUID id = UUID.randomUUID();
        Student student = sampleStudent(id, "ADM-1");
        when(studentRepository.findIncludingDeletedById(id)).thenReturn(Optional.of(student));
        when(studentDocumentRepository.findByStudent_IdOrderByUploadedDateDesc(id)).thenReturn(List.of());
        when(studentPdfRenderer.render(anyList(), anyMap(), eq("admin"), any(Instant.class)))
                .thenReturn("%PDF-mock".getBytes());

        GeneratedReport report = reportService.exportSingleStudent(id);

        assertThat(report.fileName()).isEqualTo("student-report-ADM-1.pdf");
        assertThat(report.content()).isEqualTo("%PDF-mock".getBytes());
        verify(studentPdfRenderer).render(anyList(), anyMap(), eq("admin"), any(Instant.class));
    }

    @Test
    void exportSingleStudentNotFound() {
        UUID id = UUID.randomUUID();
        when(studentRepository.findIncludingDeletedById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.exportSingleStudent(id))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void exportSelectedRejectsOverLimit() {
        List<UUID> ids = java.util.stream.IntStream.range(0, 51)
                .mapToObj(i -> UUID.randomUUID())
                .toList();

        assertThatThrownBy(() -> reportService.exportSelectedStudents(ids))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("At most 50");
    }

    @Test
    void exportFilteredRejectsWhenOverMaxResults() {
        when(studentRepository.count(any(Specification.class))).thenReturn(101L);

        assertThatThrownBy(() -> reportService.exportFilteredStudents(
                        new ReportFilterRequest(null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("maximum allowed is 100");
    }

    @Test
    void exportFilteredGeneratesPdfForActiveScope() {
        UUID id = UUID.randomUUID();
        Student student = sampleStudent(id, "ADM-F");
        when(studentRepository.count(any(Specification.class))).thenReturn(1L);
        when(studentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(student));
        when(studentDocumentRepository.findByStudent_IdOrderByUploadedDateDesc(id)).thenReturn(List.of());
        when(studentPdfRenderer.render(anyList(), anyMap(), anyString(), any(Instant.class)))
                .thenReturn("%PDF-filter".getBytes());

        GeneratedReport report = reportService.exportFilteredStudents(
                new ReportFilterRequest(
                        ReportStudentScope.ACTIVE, "Anita", Gender.FEMALE, 2024, null, null, null));

        assertThat(report.fileName()).startsWith("students-report-");
        assertThat(report.content()).isEqualTo("%PDF-filter".getBytes());
    }

    @Test
    void exportFilteredArchivedScopeUsesDeletedQuery() {
        UUID id = UUID.randomUUID();
        Student student = sampleStudent(id, "ADM-ARCH");
        student.setStatus(StudentStatus.INACTIVE);
        student.setDeleted(true);

        when(studentDeletedQuery.countDeletedMatching(
                        isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(1L);
        when(studentDeletedQuery.findDeletedMatching(
                        isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Sort.class)))
                .thenReturn(List.of(student));
        when(studentDocumentRepository.findByStudent_IdOrderByUploadedDateDesc(id)).thenReturn(List.of());
        when(studentPdfRenderer.render(anyList(), anyMap(), anyString(), any(Instant.class)))
                .thenReturn("%PDF-arch".getBytes());

        GeneratedReport report = reportService.exportFilteredStudents(
                new ReportFilterRequest(ReportStudentScope.ARCHIVED, null, null, null, null, null, null));

        assertThat(report.content()).isEqualTo("%PDF-arch".getBytes());
        verify(studentDeletedQuery).findDeletedMatching(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Sort.class));
    }

    @Test
    void exportFilteredAllScopeCombinesActiveAndArchived() {
        Student active = sampleStudent(UUID.randomUUID(), "ADM-A");
        Student archived = sampleStudent(UUID.randomUUID(), "ADM-B");
        archived.setStatus(StudentStatus.INACTIVE);
        archived.setDeleted(true);

        when(studentRepository.count(any(Specification.class))).thenReturn(1L);
        when(studentDeletedQuery.countDeletedMatching(
                        isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(1L);
        when(studentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(active));
        when(studentDeletedQuery.findDeletedMatching(
                        isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Sort.class)))
                .thenReturn(List.of(archived));
        when(studentDocumentRepository.findByStudent_IdOrderByUploadedDateDesc(any()))
                .thenReturn(List.of());
        when(studentPdfRenderer.render(anyList(), anyMap(), anyString(), any(Instant.class)))
                .thenReturn("%PDF-all".getBytes());

        GeneratedReport report = reportService.exportFilteredStudents(
                new ReportFilterRequest(ReportStudentScope.ALL, null, null, null, null, null, null));

        assertThat(report.content()).isEqualTo("%PDF-all".getBytes());
        verify(studentPdfRenderer).render(anyList(), anyMap(), anyString(), any(Instant.class));
    }

    private static Student sampleStudent(UUID id, String admissionNumber) {
        return Student.builder()
                .id(id)
                .admissionNumber(admissionNumber)
                .firstName("Anita")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(2014, 1, 1))
                .admissionDate(LocalDate.of(2024, 6, 1))
                .status(StudentStatus.ACTIVE)
                .build();
    }

    private static void authenticateAs(String username) {
        Role role = Role.builder().id(UUID.randomUUID()).name(RoleName.ADMIN).build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(username + "@oms.local")
                .passwordHash("hash")
                .authProvider(AuthProvider.LOCAL)
                .role(role)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
    }
}
