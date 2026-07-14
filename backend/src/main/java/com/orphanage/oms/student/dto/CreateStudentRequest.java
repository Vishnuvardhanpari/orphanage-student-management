package com.orphanage.oms.student.dto;

import com.orphanage.oms.student.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Payload for registering a new student (JSON part of multipart create).
 */
public record CreateStudentRequest(
        @NotBlank @Size(max = 50) String admissionNumber,
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @NotNull Gender gender,
        @NotNull @PastOrPresent LocalDate dateOfBirth,
        @Size(max = 10) String bloodGroup,
        @Size(max = 100) String religion,
        @Size(max = 100) String nationality,
        @Size(max = 12) @Pattern(regexp = "^$|^\\d{12}$", message = "Aadhaar number must be 12 digits when provided")
        String aadhaarNumber,
        @Size(max = 20) String phoneNumber,
        @Size(max = 100) String guardianName,
        @Size(max = 50) String guardianRelationship,
        @Size(max = 20) String guardianPhone,
        String guardianAddress,
        @Size(max = 255) String schoolName,
        @Size(max = 50) String standard,
        @Size(max = 50) String medium,
        @Size(max = 255) String previousSchool,
        String medicalConditions,
        String allergies,
        String disability,
        String emergencyNotes,
        @NotNull @PastOrPresent LocalDate admissionDate
) {
}
