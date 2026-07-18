package com.orphanage.oms.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orphanage.oms.dashboard.dto.DashboardGenderCountResponse;
import com.orphanage.oms.dashboard.dto.DashboardMonthlyAdmissionResponse;
import com.orphanage.oms.dashboard.dto.DashboardStatusCountResponse;
import com.orphanage.oms.dashboard.dto.DashboardSummaryResponse;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import com.orphanage.oms.student.repository.StudentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummaryAggregatesCountsAndRecentLists() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate nextMonthStart = currentMonth.plusMonths(1).atDay(1);

        when(studentRepository.countByStatus(StudentStatus.ACTIVE)).thenReturn(3L);
        when(studentRepository.countDeleted()).thenReturn(2L);
        when(studentRepository.countByGender(Gender.MALE)).thenReturn(1L);
        when(studentRepository.countByGender(Gender.FEMALE)).thenReturn(2L);
        when(studentRepository.countAdmissionsBetweenIncludingDeleted(monthStart, nextMonthStart))
                .thenReturn(4L);

        Student recent = Student.builder()
                .id(UUID.randomUUID())
                .admissionNumber("ADM-001")
                .firstName("Anita")
                .lastName("Rao")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(2014, 1, 1))
                .admissionDate(today.minusDays(2))
                .updatedDate(Instant.parse("2026-01-15T10:00:00Z"))
                .status(StudentStatus.ACTIVE)
                .build();
        when(studentRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recent)));

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertThat(summary.totalStudents()).isEqualTo(5L);
        assertThat(summary.activeStudents()).isEqualTo(3L);
        assertThat(summary.inactiveStudents()).isEqualTo(2L);
        assertThat(summary.newAdmissions()).isEqualTo(4L);
        assertThat(summary.maleStudents()).isEqualTo(1L);
        assertThat(summary.femaleStudents()).isEqualTo(2L);
        assertThat(summary.recentAdmissions()).hasSize(1);
        assertThat(summary.recentAdmissions().getFirst().admissionNumber()).isEqualTo("ADM-001");
        assertThat(summary.recentUpdates()).hasSize(1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(studentRepository, org.mockito.Mockito.times(2)).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getAllValues()).hasSize(2);
    }

    @Test
    void getAdmissionsTrendZeroFillsMissingMonths() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth firstMonth = currentMonth.minusMonths(11);
        LocalDate rangeStart = firstMonth.atDay(1);
        LocalDate rangeEndExclusive = currentMonth.plusMonths(1).atDay(1);

        when(studentRepository.countAdmissionsByMonthIncludingDeleted(
                        eq(rangeStart), eq(rangeEndExclusive)))
                .thenReturn(List.<Object[]>of(new Object[] {
                    currentMonth.getYear(), currentMonth.getMonthValue(), 5L
                }));

        List<DashboardMonthlyAdmissionResponse> trend = dashboardService.getAdmissionsTrend();

        assertThat(trend).hasSize(12);
        assertThat(trend.getFirst().yearMonth()).isEqualTo(firstMonth.toString());
        assertThat(trend.getFirst().count()).isZero();
        assertThat(trend.getLast().yearMonth()).isEqualTo(currentMonth.toString());
        assertThat(trend.getLast().count()).isEqualTo(5L);
    }

    @Test
    void getGenderDistributionIncludesZeroCountsForMissingGenders() {
        when(studentRepository.countGroupedByGender())
                .thenReturn(List.<Object[]>of(new Object[] {Gender.MALE, 2L}));

        List<DashboardGenderCountResponse> distribution = dashboardService.getGenderDistribution();

        assertThat(distribution)
                .containsExactly(
                        new DashboardGenderCountResponse(Gender.MALE, 2L),
                        new DashboardGenderCountResponse(Gender.FEMALE, 0L),
                        new DashboardGenderCountResponse(Gender.OTHER, 0L));
    }

    @Test
    void getStatusDistributionIncludesZeroCountsForMissingStatuses() {
        when(studentRepository.countGroupedByStatusIncludingDeleted())
                .thenReturn(List.<Object[]>of(new Object[] {"ACTIVE", 7L}));

        List<DashboardStatusCountResponse> distribution = dashboardService.getStatusDistribution();

        assertThat(distribution)
                .containsExactly(
                        new DashboardStatusCountResponse(StudentStatus.ACTIVE, 7L),
                        new DashboardStatusCountResponse(StudentStatus.INACTIVE, 0L));
    }
}
