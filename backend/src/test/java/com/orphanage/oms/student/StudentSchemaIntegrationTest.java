package com.orphanage.oms.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.entity.StudentDocument;
import com.orphanage.oms.student.enums.DocumentType;
import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies student schema mapping: Flyway migration presence, relationships, uniqueness, and soft-delete filtering.
 *
 * <p>Uses the standard {@code test} profile (H2 + Hibernate {@code create-drop}). PostgreSQL Flyway
 * migrations use {@code TIMESTAMPTZ} and are validated at runtime against Postgres, not H2.
 */
@SpringBootTest
@ActiveProfiles("test")
class StudentSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void flywayMigrationV4IsOnClasspath() {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V4__create_students_and_documents.sql");
        assertThat(migration.exists()).isTrue();
    }

    @Test
    void flywayMigrationV5IsOnClasspath() {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V5__admission_number_case_insensitive_unique.sql");
        assertThat(migration.exists()).isTrue();
    }

    @Test
    void hibernateCreatesStudentTables() {
        assertThat(tableExists("students")).isTrue();
        assertThat(tableExists("student_documents")).isTrue();
    }

    @Test
    @Transactional
    void persistsStudentAndDocumentRelationship() {
        Student student = persistActiveStudent("ADM-001", null);

        StudentDocument document = StudentDocument.builder()
                .student(student)
                .documentType(DocumentType.BIRTH_CERTIFICATE)
                .originalFileName("birth.pdf")
                .storedFileName("birth-stored.pdf")
                .storagePath("students/" + student.getId() + "/birth-stored.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .uploadedBy(UUID.randomUUID())
                .build();
        entityManager.persist(document);
        entityManager.flush();
        entityManager.clear();

        Student loaded = entityManager.find(Student.class, student.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getStatus()).isEqualTo(StudentStatus.ACTIVE);
        assertThat(loaded.isDeleted()).isFalse();
        assertThat(loaded.getDocuments()).hasSize(1);
        assertThat(loaded.getDocuments().getFirst().getDocumentType())
                .isEqualTo(DocumentType.BIRTH_CERTIFICATE);
        assertThat(loaded.getDocuments().getFirst().getUploadedDate()).isNotNull();
    }

    @Test
    @Transactional
    void rejectsDuplicateAdmissionNumber() {
        persistActiveStudent("ADM-DUP", null);
        entityManager.flush();

        Student duplicate = Student.builder()
                .admissionNumber("ADM-DUP")
                .firstName("Other")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(2012, 1, 1))
                .admissionDate(LocalDate.of(2020, 1, 1))
                .build();
        entityManager.persist(duplicate);

        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @Transactional
    void rejectsDuplicateAadhaarNumber() {
        persistActiveStudent("ADM-A1", "123456789012");
        entityManager.flush();

        Student duplicate = Student.builder()
                .admissionNumber("ADM-A2")
                .firstName("Other")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(2012, 1, 1))
                .aadhaarNumber("123456789012")
                .admissionDate(LocalDate.of(2020, 1, 1))
                .build();
        entityManager.persist(duplicate);

        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @Transactional
    void softDeletedStudentIsExcludedFromDefaultFind() {
        Student student = persistActiveStudent("ADM-SOFT", null);
        entityManager.flush();
        UUID id = student.getId();

        student.setDeleted(true);
        student.setDeletedDate(Instant.now());
        student.setStatus(StudentStatus.INACTIVE);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Student.class, id)).isNull();

        Integer deletedFlag = jdbcTemplate.queryForObject(
                "SELECT CASE WHEN deleted THEN 1 ELSE 0 END FROM students WHERE id = ?",
                Integer.class,
                id);
        assertThat(deletedFlag).isEqualTo(1);
    }

    @Test
    @Transactional
    void softDeletedDocumentIsExcludedFromDefaultFind() {
        Student student = persistActiveStudent("ADM-DOC", null);
        StudentDocument document = StudentDocument.builder()
                .student(student)
                .documentType(DocumentType.AADHAAR_CARD)
                .originalFileName("aadhaar.pdf")
                .storedFileName("aadhaar-stored.pdf")
                .storagePath("students/" + student.getId() + "/aadhaar-stored.pdf")
                .contentType("application/pdf")
                .fileSize(2048L)
                .build();
        entityManager.persist(document);
        entityManager.flush();
        UUID documentId = document.getId();

        document.setDeleted(true);
        document.setDeletedDate(Instant.now());
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(StudentDocument.class, documentId)).isNull();
    }

    private Student persistActiveStudent(String admissionNumber, String aadhaarNumber) {
        Student student = Student.builder()
                .admissionNumber(admissionNumber)
                .firstName("Ada")
                .lastName("Lovelace")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(2010, 5, 15))
                .aadhaarNumber(aadhaarNumber)
                .admissionDate(LocalDate.of(2020, 6, 1))
                .exitRemarks("N/A")
                .status(StudentStatus.ACTIVE)
                .build();
        entityManager.persist(student);
        return student;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE LOWER(table_name) = LOWER(?)
                """,
                Integer.class,
                tableName);
        return count != null && count > 0;
    }
}
