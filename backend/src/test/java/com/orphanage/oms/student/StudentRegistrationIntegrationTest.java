package com.orphanage.oms.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orphanage.oms.auth.repository.RefreshTokenRepository;
import com.orphanage.oms.student.entity.Student;
import com.orphanage.oms.student.repository.StudentDocumentRepository;
import com.orphanage.oms.student.repository.StudentRepository;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import com.orphanage.oms.user.repository.RoleRepository;
import com.orphanage.oms.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentRegistrationIntegrationTest {

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
    private StudentDocumentRepository studentDocumentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        studentDocumentRepository.deleteAll();
        studentRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        var adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
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
    }

    @Test
    void staffCanRegisterStudentWithPhotoAndDocument() throws Exception {
        String token = loginAccessToken("staff", "StaffPass123!");

        String json = """
                {
                  "admissionNumber":"ADM-100",
                  "firstName":"Anita",
                  "lastName":"Sharma",
                  "gender":"FEMALE",
                  "dateOfBirth":"2014-03-15",
                  "guardianName":"Ramesh Sharma",
                  "guardianRelationship":"Father",
                  "guardianPhone":"9876543210",
                  "schoolName":"City School",
                  "standard":"6",
                  "medium":"English",
                  "admissionDate":"2024-06-01"
                }
                """;

        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "photo.jpg",
                "image/jpeg",
                new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});
        MockMultipartFile document = new MockMultipartFile(
                "documents",
                "birth.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/api/v1/students")
                        .file(data)
                        .file(photo)
                        .file(document)
                        .param("documentTypes", "BIRTH_CERTIFICATE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.admissionNumber").value("ADM-100"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        UUID id = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());

        Student saved = studentRepository.findById(id).orElseThrow();
        assertThat(saved.getFirstName()).isEqualTo("Anita");
        assertThat(saved.getProfilePhotoPath()).contains("profile-photo.jpg");
        assertThat(studentDocumentRepository.findAll()).hasSize(1);
    }

    @Test
    void duplicateAdmissionNumberReturnsConflict() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        registerMinimal(token, "ADM-200");

        String json = """
                {
                  "admissionNumber":"ADM-200",
                  "firstName":"Other",
                  "gender":"MALE",
                  "dateOfBirth":"2012-01-01",
                  "admissionDate":"2024-01-01"
                }
                """;
        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/students")
                        .file(data)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Admission number is already in use."));
    }

    @Test
    void duplicateAdmissionNumberDifferentCaseReturnsConflict() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        registerMinimal(token, "ADM-201");

        String json = """
                {
                  "admissionNumber":"adm-201",
                  "firstName":"Other",
                  "gender":"MALE",
                  "dateOfBirth":"2012-01-01",
                  "admissionDate":"2024-01-01"
                }
                """;
        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/students")
                        .file(data)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Admission number is already in use."));
    }

    @Test
    void softDeletedAdmissionNumberReuseReturnsConflict() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        registerMinimal(token, "ADM-SOFT");

        Student saved = studentRepository.findAll().stream()
                .filter(s -> "ADM-SOFT".equals(s.getAdmissionNumber()))
                .findFirst()
                .orElseThrow();
        jdbcTemplate.update(
                "UPDATE students SET deleted = TRUE, status = 'INACTIVE' WHERE id = ?",
                saved.getId());

        String json = """
                {
                  "admissionNumber":"ADM-SOFT",
                  "firstName":"Reuse",
                  "gender":"FEMALE",
                  "dateOfBirth":"2012-01-01",
                  "admissionDate":"2024-01-01"
                }
                """;
        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/students")
                        .file(data)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Admission number is already in use."));
    }

    @Test
    void dateOfBirthAfterAdmissionDateReturnsBadRequest() throws Exception {
        String token = loginAccessToken("staff", "StaffPass123!");
        String json = """
                {
                  "admissionNumber":"ADM-DOB",
                  "firstName":"Test",
                  "gender":"MALE",
                  "dateOfBirth":"2020-01-01",
                  "admissionDate":"2019-01-01"
                }
                """;
        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/students")
                        .file(data)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").value(
                        "Date of birth must be on or before admission date."));
    }

    @Test
    void spoofedPhotoContentReturnsBadRequest() throws Exception {
        String token = loginAccessToken("staff", "StaffPass123!");
        String json = """
                {
                  "admissionNumber":"ADM-SPOOF",
                  "firstName":"Test",
                  "gender":"MALE",
                  "dateOfBirth":"2012-01-01",
                  "admissionDate":"2024-01-01"
                }
                """;
        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "photo.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3, 4});

        mockMvc.perform(multipart("/api/v1/students")
                        .file(data)
                        .file(photo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("content does not match")));
    }

    @Test
    void unauthenticatedCannotRegister() throws Exception {
        String json = """
                {
                  "admissionNumber":"ADM-300",
                  "firstName":"X",
                  "gender":"OTHER",
                  "dateOfBirth":"2010-01-01",
                  "admissionDate":"2024-01-01"
                }
                """;
        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/students").file(data))
                .andExpect(status().isUnauthorized());
    }

    private void registerMinimal(String token, String admissionNumber) throws Exception {
        String json = """
                {
                  "admissionNumber":"%s",
                  "firstName":"First",
                  "gender":"MALE",
                  "dateOfBirth":"2011-01-01",
                  "admissionDate":"2024-01-01"
                }
                """.formatted(admissionNumber);
        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/students")
                        .file(data)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
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
