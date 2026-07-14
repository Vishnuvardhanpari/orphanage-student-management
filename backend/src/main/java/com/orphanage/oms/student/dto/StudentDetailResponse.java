package com.orphanage.oms.student.dto;

import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Full student profile for read APIs. Does not expose storage paths.
 */
public record StudentDetailResponse(
        UUID id,
        String admissionNumber,
        String firstName,
        String lastName,
        Gender gender,
        LocalDate dateOfBirth,
        String bloodGroup,
        String religion,
        String nationality,
        String aadhaarNumber,
        String phoneNumber,
        String guardianName,
        String guardianRelationship,
        String guardianPhone,
        String guardianAddress,
        String schoolName,
        String standard,
        String medium,
        String previousSchool,
        String medicalConditions,
        String allergies,
        String disability,
        String emergencyNotes,
        LocalDate admissionDate,
        LocalDate exitDate,
        String exitReason,
        String exitRemarks,
        StudentStatus status,
        boolean hasProfilePhoto,
        Instant createdDate,
        Instant updatedDate
) {
}
