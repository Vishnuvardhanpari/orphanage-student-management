package com.orphanage.oms.student.entity;

import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Primary student record spanning personal, guardian, education, medical, and exit data.
 *
 * <p>Default queries exclude soft-deleted rows via {@code @SQLRestriction}.
 * Admin restore / historical queries in later milestones must bypass this filter explicitly.
 */
@Entity
@Table(name = "students")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "admission_number", nullable = false, unique = true, length = 50)
    private String admissionNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(length = 100)
    private String religion;

    @Column(length = 100)
    private String nationality;

    @Column(name = "aadhaar_number", unique = true, length = 12)
    private String aadhaarNumber;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "guardian_name", length = 100)
    private String guardianName;

    @Column(name = "guardian_relationship", length = 50)
    private String guardianRelationship;

    @Column(name = "guardian_phone", length = 20)
    private String guardianPhone;

    @Column(name = "guardian_address", columnDefinition = "TEXT")
    private String guardianAddress;

    @Column(name = "school_name", length = 255)
    private String schoolName;

    @Column(length = 50)
    private String standard;

    @Column(length = 50)
    private String medium;

    @Column(name = "previous_school", length = 255)
    private String previousSchool;

    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    private String medicalConditions;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String disability;

    @Column(name = "emergency_notes", columnDefinition = "TEXT")
    private String emergencyNotes;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Column(name = "exit_reason", columnDefinition = "TEXT")
    private String exitReason;

    @Column(name = "exit_remarks", columnDefinition = "TEXT")
    private String exitRemarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(name = "profile_photo_path", length = 500)
    private String profilePhotoPath;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "deleted_date")
    private Instant deletedDate;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_date", nullable = false)
    private Instant updatedDate;

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    @Builder.Default
    private List<StudentDocument> documents = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdDate = now;
        updatedDate = now;
        if (status == null) {
            status = StudentStatus.ACTIVE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedDate = Instant.now();
    }
}
