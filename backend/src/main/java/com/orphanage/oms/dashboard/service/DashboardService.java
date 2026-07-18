package com.orphanage.oms.dashboard.service;

import com.orphanage.oms.dashboard.dto.DashboardGenderCountResponse;
import com.orphanage.oms.dashboard.dto.DashboardMonthlyAdmissionResponse;
import com.orphanage.oms.dashboard.dto.DashboardRecentStudentResponse;
import com.orphanage.oms.dashboard.dto.DashboardStatusCountResponse;
import com.orphanage.oms.dashboard.dto.DashboardSummaryResponse;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import com.orphanage.oms.student.repository.StudentRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates student statistics for the executive dashboard.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int RECENT_LIMIT = 5;
    private static final int ADMISSION_TREND_MONTHS = 12;

    private final StudentRepository studentRepository;

    public DashboardService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public DashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate nextMonthStart = currentMonth.plusMonths(1).atDay(1);

        long activeStudents = studentRepository.countByStatus(StudentStatus.ACTIVE);
        long inactiveStudents = studentRepository.countDeleted();
        long maleStudents = studentRepository.countByGender(Gender.MALE);
        long femaleStudents = studentRepository.countByGender(Gender.FEMALE);
        long newAdmissions = studentRepository.countAdmissionsBetweenIncludingDeleted(
                monthStart, nextMonthStart);

        List<DashboardRecentStudentResponse> recentAdmissions = studentRepository
                .findAll(PageRequest.of(0, RECENT_LIMIT, Sort.by(Sort.Direction.DESC, "admissionDate")))
                .map(this::toRecent)
                .getContent();

        List<DashboardRecentStudentResponse> recentUpdates = studentRepository
                .findAll(PageRequest.of(0, RECENT_LIMIT, Sort.by(Sort.Direction.DESC, "updatedDate")))
                .map(this::toRecent)
                .getContent();

        return new DashboardSummaryResponse(
                activeStudents + inactiveStudents,
                activeStudents,
                inactiveStudents,
                newAdmissions,
                maleStudents,
                femaleStudents,
                recentAdmissions,
                recentUpdates);
    }

    public List<DashboardMonthlyAdmissionResponse> getAdmissionsTrend() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth firstMonth = currentMonth.minusMonths(ADMISSION_TREND_MONTHS - 1L);
        LocalDate rangeStart = firstMonth.atDay(1);
        LocalDate rangeEndExclusive = currentMonth.plusMonths(1).atDay(1);

        Map<String, Long> byMonth = new HashMap<>();
        for (Object[] row : studentRepository.countAdmissionsByMonthIncludingDeleted(
                rangeStart, rangeEndExclusive)) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            byMonth.put(YearMonth.of(year, month).toString(), count);
        }

        List<DashboardMonthlyAdmissionResponse> series = new ArrayList<>(ADMISSION_TREND_MONTHS);
        for (int i = 0; i < ADMISSION_TREND_MONTHS; i++) {
            YearMonth month = firstMonth.plusMonths(i);
            String key = month.toString();
            series.add(new DashboardMonthlyAdmissionResponse(key, byMonth.getOrDefault(key, 0L)));
        }
        return series;
    }

    public List<DashboardGenderCountResponse> getGenderDistribution() {
        Map<Gender, Long> counts = new EnumMap<>(Gender.class);
        for (Gender gender : Gender.values()) {
            counts.put(gender, 0L);
        }
        for (Object[] row : studentRepository.countGroupedByGender()) {
            counts.put((Gender) row[0], ((Number) row[1]).longValue());
        }
        List<DashboardGenderCountResponse> result = new ArrayList<>(Gender.values().length);
        for (Gender gender : Gender.values()) {
            result.add(new DashboardGenderCountResponse(gender, counts.get(gender)));
        }
        return result;
    }

    public List<DashboardStatusCountResponse> getStatusDistribution() {
        Map<StudentStatus, Long> counts = new EnumMap<>(StudentStatus.class);
        for (StudentStatus status : StudentStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : studentRepository.countGroupedByStatusIncludingDeleted()) {
            StudentStatus status = StudentStatus.valueOf(String.valueOf(row[0]));
            counts.put(status, ((Number) row[1]).longValue());
        }

        List<DashboardStatusCountResponse> result = new ArrayList<>(StudentStatus.values().length);
        for (StudentStatus status : StudentStatus.values()) {
            result.add(new DashboardStatusCountResponse(status, counts.get(status)));
        }
        return result;
    }

    private DashboardRecentStudentResponse toRecent(Student student) {
        return new DashboardRecentStudentResponse(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getAdmissionNumber(),
                student.getAdmissionDate(),
                student.getUpdatedDate());
    }
}
