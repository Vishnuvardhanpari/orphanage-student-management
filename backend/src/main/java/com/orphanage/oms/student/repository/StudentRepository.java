package com.orphanage.oms.student.repository;

import com.orphanage.oms.student.entity.Student;
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
}
