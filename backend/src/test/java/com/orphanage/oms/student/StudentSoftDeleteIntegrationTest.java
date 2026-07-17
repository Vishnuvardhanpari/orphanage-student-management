package com.orphanage.oms.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orphanage.oms.auth.repository.RefreshTokenRepository;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.enums.Gender;
import com.orphanage.oms.student.enums.StudentStatus;
import com.orphanage.oms.student.repository.StudentRepository;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import com.orphanage.oms.user.repository.RoleRepository;
import com.orphanage.oms.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentSoftDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM student_documents");
        jdbcTemplate.update("DELETE FROM students");
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        var adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        var staffRole = roleRepository.findByName(RoleName.STAFF).orElseThrow();

        userRepository.save(User.builder()
                .username("admin")
                .email("admin@oms.local")
                .passwordHash(passwordEncoder.encode("ChangeMeAdmin123!"))
                .authProvider(AuthProvider.LOCAL)
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build());
        userRepository.save(User.builder()
                .username("staff")
                .email("staff@oms.local")
                .passwordHash(passwordEncoder.encode("StaffPass123!"))
                .authProvider(AuthProvider.LOCAL)
                .role(staffRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build());

        adminToken = loginAccessToken("admin", "ChangeMeAdmin123!");
        staffToken = loginAccessToken("staff", "StaffPass123!");
    }

    @Test
    void staffCanSoftDeleteAndStudentDisappearsFromActiveList() throws Exception {
        UUID id = persistStudent(b -> b.admissionNumber("ADM-SD-001").firstName("Anita"));

        mockMvc.perform(delete("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNoContent());

        Integer deleted = jdbcTemplate.queryForObject(
                "SELECT CASE WHEN deleted THEN 1 ELSE 0 END FROM students WHERE id = ?",
                Integer.class,
                id);
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM students WHERE id = ?", String.class, id);
        assertThat(deleted).isEqualTo(1);
        assertThat(status).isEqualTo("INACTIVE");

        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // Regression for QA BUG-005: DELETE must accept an optional JSON body
    // recording exit details, and persist them on the archived record.
    @Test
    void softDeleteRecordsOptionalExitDetails() throws Exception {
        UUID id = persistStudent(b -> b.admissionNumber("ADM-SD-008")
                .admissionDate(LocalDate.of(2024, 6, 1)));

        mockMvc.perform(delete("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exitDate": "2026-01-10",
                                  "exitReason": "Family relocated",
                                  "exitRemarks": "Handed over to guardian"
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitDate").value("2026-01-10"))
                .andExpect(jsonPath("$.exitReason").value("Family relocated"))
                .andExpect(jsonPath("$.exitRemarks").value("Handed over to guardian"));
    }

    @Test
    void softDeleteWithoutBodyLeavesExitFieldsNull() throws Exception {
        UUID id = persistStudent(b -> b.admissionNumber("ADM-SD-009"));

        mockMvc.perform(delete("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitDate").doesNotExist())
                .andExpect(jsonPath("$.exitReason").doesNotExist());
    }

    @Test
    void softDeleteRejectsExitDateBeforeAdmissionDate() throws Exception {
        UUID id = persistStudent(b -> b.admissionNumber("ADM-SD-010")
                .admissionDate(LocalDate.of(2024, 6, 1)));

        mockMvc.perform(delete("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "exitDate": "2023-01-01" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Exit date must be on or after the admission date."));
    }

    @Test
    void softDeleteRejectsFutureExitDate() throws Exception {
        UUID id = persistStudent(b -> b.admissionNumber("ADM-SD-011"));

        mockMvc.perform(delete("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "exitDate": "2099-01-01" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void softDeleteUnknownOrAlreadyDeletedReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/students/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNotFound());

        UUID id = persistStudent(b -> b.admissionNumber("ADM-SD-002"));
        mockMvc.perform(delete("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveListAndArchivedProfileReadableByStaff() throws Exception {
        UUID id = persistStudent(b -> b.admissionNumber("ADM-SD-003").firstName("Archived"));
        mockMvc.perform(delete("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/students/inactive")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].admissionNumber").value("ADM-SD-003"))
                .andExpect(jsonPath("$.content[0].status").value("INACTIVE"))
                .andExpect(jsonPath("$.content[0].deletedDate").isNotEmpty());

        mockMvc.perform(get("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionNumber").value("ADM-SD-003"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void mutationsOnArchivedStudentReturn404() throws Exception {
        UUID id = persistStudent(b -> b.admissionNumber("ADM-SD-004"));
        mockMvc.perform(delete("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Nope",
                                  "gender": "FEMALE",
                                  "dateOfBirth": "2014-03-15",
                                  "admissionDate": "2024-06-01"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffCannotRestoreButAdminCan() throws Exception {
        UUID id = persistStudent(b -> b.admissionNumber("ADM-SD-005").firstName("RestoreMe"));
        mockMvc.perform(delete("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/v1/students/" + id + "/restore")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/students/" + id + "/restore")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.admissionNumber").value("ADM-SD-005"));

        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void restoreActiveStudentReturns409() throws Exception {
        UUID id = persistStudent(b -> b.admissionNumber("ADM-SD-006"));

        mockMvc.perform(patch("/api/v1/students/" + id + "/restore")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Student is not archived and cannot be restored."));
    }

    @Test
    void restoreUnknownReturns404() throws Exception {
        mockMvc.perform(patch("/api/v1/students/" + UUID.randomUUID() + "/restore")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // Regression for QA BUG-002: sorting the archived list by School used to
    // 400 because schoolName was missing from the inactive-list sort whitelist.
    @Test
    void inactiveListSortingBySchoolNameIsAccepted() throws Exception {
        UUID id = persistStudent(b -> b.admissionNumber("ADM-SD-007").schoolName("Green Valley School"));
        mockMvc.perform(delete("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/students/inactive?sort=schoolName,asc")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void inactiveListUnsupportedSortPropertyReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/students/inactive?sort=aadhaarNumber,asc")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported sort property: aadhaarNumber"));
    }

    private UUID persistStudent(java.util.function.Consumer<Student.StudentBuilder> customizer) {
        Student.StudentBuilder builder = Student.builder()
                .firstName("Student")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(2014, 3, 15))
                .admissionDate(LocalDate.of(2024, 6, 1))
                .status(StudentStatus.ACTIVE)
                .deleted(false);
        customizer.accept(builder);
        return studentRepository.save(builder.build()).getId();
    }

    private String loginAccessToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }
}
