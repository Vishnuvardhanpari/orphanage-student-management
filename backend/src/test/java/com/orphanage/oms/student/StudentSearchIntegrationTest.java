package com.orphanage.oms.student;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class StudentSearchIntegrationTest {

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

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        // Native deletes bypass @SQLRestriction so soft-deleted rows are purged too.
        jdbcTemplate.update("DELETE FROM student_documents");
        jdbcTemplate.update("DELETE FROM students");
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        var staffRole = roleRepository.findByName(RoleName.STAFF).orElseThrow();
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

        token = loginAccessToken("staff", "StaffPass123!");
    }

    @Test
    void listReturnsPaginationEnvelope() throws Exception {
        persistStudent(b -> b.admissionNumber("ADM-S-001").firstName("Anita")
                .admissionDate(LocalDate.of(2024, 6, 1)));
        persistStudent(b -> b.admissionNumber("ADM-S-002").firstName("Bala")
                .admissionDate(LocalDate.of(2024, 7, 1)));
        persistStudent(b -> b.admissionNumber("ADM-S-003").firstName("Chitra")
                .admissionDate(LocalDate.of(2024, 8, 1)));

        mockMvc.perform(authorized("/api/v1/students?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content.length()").value(2))
                // Default sort: admissionDate desc.
                .andExpect(jsonPath("$.content[0].admissionNumber").value("ADM-S-003"))
                .andExpect(jsonPath("$.content[1].admissionNumber").value("ADM-S-002"))
                .andExpect(jsonPath("$.content[0].firstName").value("Chitra"))
                .andExpect(jsonPath("$.content[0].storagePath").doesNotExist());
    }

    @Test
    void searchMatchesAllConfiguredFieldsCaseInsensitively() throws Exception {
        persistStudent(b -> b.admissionNumber("ADM-S-010").firstName("Anita").lastName("Sharma")
                .aadhaarNumber("123456789012")
                .guardianName("Ramesh Kumar")
                .phoneNumber("9876543210"));
        persistStudent(b -> b.admissionNumber("ADM-S-011").firstName("Bala"));

        expectSingleMatch("search=aNiTa", "ADM-S-010");
        expectSingleMatch("search=sharma", "ADM-S-010");
        expectSingleMatch("search=anita sharma", "ADM-S-010");
        expectSingleMatch("search=adm-s-010", "ADM-S-010");
        expectSingleMatch("search=345678", "ADM-S-010");
        expectSingleMatch("search=ramesh", "ADM-S-010");
        expectSingleMatch("search=98765", "ADM-S-010");

        mockMvc.perform(authorized("/api/v1/students?search=no-such-student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void filtersCombine() throws Exception {
        persistStudent(b -> b.admissionNumber("ADM-S-020").firstName("Anita").gender(Gender.FEMALE)
                .schoolName("Green Valley School").admissionDate(LocalDate.of(2024, 6, 1)));
        persistStudent(b -> b.admissionNumber("ADM-S-021").firstName("Bala").gender(Gender.MALE)
                .schoolName("Green Valley School").admissionDate(LocalDate.of(2024, 6, 1)));
        persistStudent(b -> b.admissionNumber("ADM-S-022").firstName("Chitra").gender(Gender.FEMALE)
                .schoolName("Sunrise Public School").admissionDate(LocalDate.of(2024, 6, 1)));
        persistStudent(b -> b.admissionNumber("ADM-S-023").firstName("Divya").gender(Gender.FEMALE)
                .schoolName("Green Valley School").admissionDate(LocalDate.of(2023, 6, 1)));

        mockMvc.perform(authorized(
                        "/api/v1/students?gender=FEMALE&status=ACTIVE&admissionYear=2024&school=green valley"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].admissionNumber").value("ADM-S-020"));
    }

    @Test
    void ageRangeTranslatesToDateOfBirthWindow() throws Exception {
        LocalDate today = LocalDate.now();
        // Exactly 10 years old today — inside ageMin=10.
        persistStudent(b -> b.admissionNumber("ADM-S-030").firstName("ExactlyTen")
                .dateOfBirth(today.minusYears(10)));
        // One day short of 10 — still 9 years old, outside the window.
        persistStudent(b -> b.admissionNumber("ADM-S-031").firstName("AlmostTen")
                .dateOfBirth(today.minusYears(10).plusDays(1)));
        // 14 years old (one day before 15th birthday) — inside ageMax=14.
        persistStudent(b -> b.admissionNumber("ADM-S-032").firstName("StillFourteen")
                .dateOfBirth(today.minusYears(15).plusDays(1)));
        // Exactly 15 years old today — outside ageMax=14.
        persistStudent(b -> b.admissionNumber("ADM-S-033").firstName("ExactlyFifteen")
                .dateOfBirth(today.minusYears(15)));

        mockMvc.perform(authorized("/api/v1/students?ageMin=10&ageMax=14&sort=admissionNumber,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].admissionNumber").value("ADM-S-030"))
                .andExpect(jsonPath("$.content[1].admissionNumber").value("ADM-S-032"));
    }

    @Test
    void admissionYearMatchesCalendarYearBoundaries() throws Exception {
        persistStudent(b -> b.admissionNumber("ADM-S-040").firstName("JanFirst")
                .admissionDate(LocalDate.of(2024, 1, 1)));
        persistStudent(b -> b.admissionNumber("ADM-S-041").firstName("DecLast")
                .admissionDate(LocalDate.of(2024, 12, 31)));
        persistStudent(b -> b.admissionNumber("ADM-S-042").firstName("NextYear")
                .admissionDate(LocalDate.of(2025, 1, 1)));

        mockMvc.perform(authorized("/api/v1/students?admissionYear=2024&sort=admissionNumber,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].admissionNumber").value("ADM-S-040"))
                .andExpect(jsonPath("$.content[1].admissionNumber").value("ADM-S-041"));
    }

    @Test
    void sortingByFirstNameWorksInBothDirections() throws Exception {
        persistStudent(b -> b.admissionNumber("ADM-S-050").firstName("Bala"));
        persistStudent(b -> b.admissionNumber("ADM-S-051").firstName("Anita"));

        mockMvc.perform(authorized("/api/v1/students?sort=firstName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("Anita"));

        mockMvc.perform(authorized("/api/v1/students?sort=firstName,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("Bala"));
    }

    @Test
    void softDeletedStudentsAreExcluded() throws Exception {
        UUID deletedId = persistStudent(b -> b.admissionNumber("ADM-S-060").firstName("Ghost"));
        persistStudent(b -> b.admissionNumber("ADM-S-061").firstName("Visible"));
        jdbcTemplate.update("UPDATE students SET deleted = TRUE WHERE id = ?", deletedId);

        mockMvc.perform(authorized("/api/v1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].admissionNumber").value("ADM-S-061"));
    }

    @Test
    void exactAdmissionNumberMatchIsCaseInsensitive() throws Exception {
        persistStudent(b -> b.admissionNumber("ADM-S-080").firstName("Exact"));
        persistStudent(b -> b.admissionNumber("ADM-S-081").firstName("Other"));

        mockMvc.perform(authorized("/api/v1/students?admissionNumber=adm-s-080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].admissionNumber").value("ADM-S-080"))
                .andExpect(jsonPath("$.content[0].firstName").value("Exact"));
    }

    @Test
    void unsupportedSortPropertyReturns400() throws Exception {
        mockMvc.perform(authorized("/api/v1/students?sort=aadhaarNumber,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported sort property: aadhaarNumber"));
    }

    // Regression for QA BUG-002: sorting by School/Standard used to 400 because
    // these grid-sortable columns were missing from the backend sort whitelist.
    @Test
    void sortingBySchoolNameWorksInBothDirections() throws Exception {
        persistStudent(b -> b.admissionNumber("ADM-S-070").firstName("Zed").schoolName("Zenith School"));
        persistStudent(b -> b.admissionNumber("ADM-S-071").firstName("Amy").schoolName("Alpha School"));

        mockMvc.perform(authorized("/api/v1/students?sort=schoolName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].schoolName").value("Alpha School"));

        mockMvc.perform(authorized("/api/v1/students?sort=schoolName,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].schoolName").value("Zenith School"));
    }

    @Test
    void sortingByStandardIsAccepted() throws Exception {
        persistStudent(b -> b.admissionNumber("ADM-S-072").standard("5"));

        mockMvc.perform(authorized("/api/v1/students?sort=standard,asc"))
                .andExpect(status().isOk());
    }

    @Test
    void invalidAgeRangeReturns400() throws Exception {
        mockMvc.perform(authorized("/api/v1/students?ageMin=12&ageMax=8"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ageMin must be less than or equal to ageMax."));

        mockMvc.perform(authorized("/api/v1/students?ageMin=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Age filters must not be negative."));
    }

    @Test
    void invalidGenderFilterReturns400() throws Exception {
        mockMvc.perform(authorized("/api/v1/students?gender=UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'gender'."));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/students"))
                .andExpect(status().isUnauthorized());
    }

    private void expectSingleMatch(String query, String expectedAdmissionNumber) throws Exception {
        mockMvc.perform(authorized("/api/v1/students?" + query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].admissionNumber").value(expectedAdmissionNumber));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authorized(
            String uri) {
        return get(uri).header("Authorization", "Bearer " + token);
    }

    private UUID persistStudent(
            java.util.function.Consumer<Student.StudentBuilder> customizer) {
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
