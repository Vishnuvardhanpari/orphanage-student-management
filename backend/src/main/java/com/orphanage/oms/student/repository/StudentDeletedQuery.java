package com.orphanage.oms.student.repository;

import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.enums.Gender;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Native queries for soft-deleted students (bypasses {@code @SQLRestriction}).
 */
public interface StudentDeletedQuery {

    Page<Student> findDeletedMatching(
            String search,
            Gender gender,
            Integer admissionYear,
            String school,
            LocalDate dateOfBirthMinExclusive,
            LocalDate dateOfBirthMaxInclusive,
            Pageable pageable);

    List<Student> findDeletedMatching(
            String search,
            Gender gender,
            Integer admissionYear,
            String school,
            LocalDate dateOfBirthMinExclusive,
            LocalDate dateOfBirthMaxInclusive,
            Sort sort);

    long countDeletedMatching(
            String search,
            Gender gender,
            Integer admissionYear,
            String school,
            LocalDate dateOfBirthMinExclusive,
            LocalDate dateOfBirthMaxInclusive);
}
