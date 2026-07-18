package com.orphanage.oms.student.repository;

import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for student records.
 */
public interface StudentRepository
        extends JpaRepository<Student, UUID>, JpaSpecificationExecutor<Student> {

    /**
     * Load by id including soft-deleted rows (bypasses {@code @SQLRestriction}).
     */
    @Query(
            value = """
                    SELECT * FROM students
                    WHERE id = :id
                    """,
            nativeQuery = true)
    Optional<Student> findIncludingDeletedById(@Param("id") UUID id);

    /**
     * Paginated soft-deleted students (bypasses {@code @SQLRestriction}).
     *
     * <p>Callers must pass {@link Pageable} sort properties using SQL column names
     * (e.g. {@code deleted_date}), not entity field names.
     */
    @Query(
            value = """
                    SELECT * FROM students
                    WHERE deleted = true
                    """,
            countQuery = """
                    SELECT COUNT(1) FROM students
                    WHERE deleted = true
                    """,
            nativeQuery = true)
    Page<Student> findAllDeleted(Pageable pageable);

    /**
     * Uniqueness check including soft-deleted rows (bypasses {@code @SQLRestriction}).
     */
    default boolean existsByAdmissionNumberIgnoreCaseIncludingDeleted(String admissionNumber) {
        return countByAdmissionNumberIgnoreCaseIncludingDeleted(admissionNumber) > 0;
    }

    /**
     * Uniqueness check including soft-deleted rows (bypasses {@code @SQLRestriction}).
     */
    default boolean existsByAadhaarNumberIncludingDeleted(String aadhaarNumber) {
        return countByAadhaarNumberIncludingDeleted(aadhaarNumber) > 0;
    }

    @Query(
            value = """
                    SELECT COUNT(1) FROM students
                    WHERE LOWER(admission_number) = LOWER(:admissionNumber)
                    """,
            nativeQuery = true)
    long countByAdmissionNumberIgnoreCaseIncludingDeleted(
            @Param("admissionNumber") String admissionNumber);

    @Query(
            value = """
                    SELECT COUNT(1) FROM students
                    WHERE aadhaar_number = :aadhaarNumber
                    """,
            nativeQuery = true)
    long countByAadhaarNumberIncludingDeleted(@Param("aadhaarNumber") String aadhaarNumber);

    /**
     * Uniqueness check including soft-deleted rows, excluding the current student.
     */
    default boolean existsByAadhaarNumberIncludingDeletedExcludingId(
            String aadhaarNumber, UUID excludeId) {
        return countByAadhaarNumberIncludingDeletedExcludingId(aadhaarNumber, excludeId) > 0;
    }

    @Query(
            value = """
                    SELECT COUNT(1) FROM students
                    WHERE aadhaar_number = :aadhaarNumber
                      AND id <> :excludeId
                    """,
            nativeQuery = true)
    long countByAadhaarNumberIncludingDeletedExcludingId(
            @Param("aadhaarNumber") String aadhaarNumber, @Param("excludeId") UUID excludeId);

    /**
     * Active (non-deleted) students with the given status.
     */
    long countByStatus(StudentStatus status);

    /**
     * Active (non-deleted) students with the given gender.
     */
    long countByGender(Gender gender);

    /**
     * Soft-deleted student count (bypasses {@code @SQLRestriction}).
     */
    @Query(
            value = """
                    SELECT COUNT(1) FROM students
                    WHERE deleted = true
                    """,
            nativeQuery = true)
    long countDeleted();

    /**
     * Admissions in {@code [startInclusive, endExclusive)} including soft-deleted rows.
     */
    @Query(
            value = """
                    SELECT COUNT(1) FROM students
                    WHERE admission_date >= :startInclusive
                      AND admission_date < :endExclusive
                    """,
            nativeQuery = true)
    long countAdmissionsBetweenIncludingDeleted(
            @Param("startInclusive") LocalDate startInclusive,
            @Param("endExclusive") LocalDate endExclusive);

    /**
     * Gender breakdown of active (non-deleted) students.
     *
     * <p>Each row is {@code [gender, count]}.
     */
    @Query(
            """
            SELECT s.gender, COUNT(s)
            FROM Student s
            GROUP BY s.gender
            """)
    List<Object[]> countGroupedByGender();

    /**
     * Status breakdown of all retained students (bypasses {@code @SQLRestriction}).
     *
     * <p>Each row is {@code [status, count]}.
     */
    @Query(
            value = """
                    SELECT status, COUNT(1)
                    FROM students
                    GROUP BY status
                    """,
            nativeQuery = true)
    List<Object[]> countGroupedByStatusIncludingDeleted();

    /**
     * Monthly admission counts in {@code [startInclusive, endExclusive)} including soft-deleted.
     *
     * <p>Each row is {@code [year, month, count]} (numeric year/month).
     */
    @Query(
            value = """
                    SELECT EXTRACT(YEAR FROM admission_date),
                           EXTRACT(MONTH FROM admission_date),
                           COUNT(1)
                    FROM students
                    WHERE admission_date >= :startInclusive
                      AND admission_date < :endExclusive
                    GROUP BY EXTRACT(YEAR FROM admission_date),
                             EXTRACT(MONTH FROM admission_date)
                    ORDER BY EXTRACT(YEAR FROM admission_date),
                             EXTRACT(MONTH FROM admission_date)
                    """,
            nativeQuery = true)
    List<Object[]> countAdmissionsByMonthIncludingDeleted(
            @Param("startInclusive") LocalDate startInclusive,
            @Param("endExclusive") LocalDate endExclusive);
}
