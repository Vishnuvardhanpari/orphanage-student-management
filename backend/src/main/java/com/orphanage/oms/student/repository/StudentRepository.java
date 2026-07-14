package com.orphanage.oms.student.repository;

import com.orphanage.oms.student.entity.Student;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for student records.
 */
public interface StudentRepository extends JpaRepository<Student, UUID> {

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
