package com.orphanage.oms.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orphanage.oms.auth.repository.RefreshTokenRepository;
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
import org.springframework.http.HttpHeaders;
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
class StudentProfileIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Native deletes bypass @SQLRestriction so soft-deleted rows are purged too.
        jdbcTemplate.update("DELETE FROM student_documents");
        jdbcTemplate.update("DELETE FROM students");
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
    void staffCanViewProfileListDocumentsDownloadAndPhoto() throws Exception {
        String token = loginAccessToken("staff", "StaffPass123!");
        UUID studentId = registerWithFiles(token);

        mockMvc.perform(get("/api/v1/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentId.toString()))
                .andExpect(jsonPath("$.admissionNumber").value("ADM-P-100"))
                .andExpect(jsonPath("$.firstName").value("Anita"))
                .andExpect(jsonPath("$.hasProfilePhoto").value(true))
                .andExpect(jsonPath("$.profilePhotoPath").doesNotExist());

        MvcResult docsResult = mockMvc.perform(get("/api/v1/students/{id}/documents", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentType").value("AADHAAR_CARD"))
                .andExpect(jsonPath("$[0].originalFileName").value("aadhaar.pdf"))
                .andExpect(jsonPath("$[0].storagePath").doesNotExist())
                .andReturn();

        UUID documentId = UUID.fromString(
                objectMapper.readTree(docsResult.getResponse().getContentAsString()).get(0).get("id").asText());

        MvcResult download = mockMvc.perform(get(
                                "/api/v1/students/{id}/documents/{documentId}/download",
                                studentId,
                                documentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("aadhaar.pdf")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andReturn();
        assertThat(download.getResponse().getContentAsByteArray())
                .isEqualTo("%PDF-1.4 sample".getBytes(StandardCharsets.UTF_8));

        MvcResult photo = mockMvc.perform(get("/api/v1/students/{id}/photo", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE))
                .andReturn();
        assertThat(photo.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void getUnknownStudentReturns404() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        mockMvc.perform(get("/api/v1/students/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student not found."));
    }

    @Test
    void downloadWrongDocumentReturns404() throws Exception {
        String token = loginAccessToken("staff", "StaffPass123!");
        UUID studentId = registerWithFiles(token);

        mockMvc.perform(get(
                                "/api/v1/students/{id}/documents/{documentId}/download",
                                studentId,
                                UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Document not found."));
    }

    @Test
    void unauthenticatedCannotViewProfile() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private UUID registerWithFiles(String token) throws Exception {
        String json = """
                {
                  "admissionNumber":"ADM-P-100",
                  "firstName":"Anita",
                  "lastName":"Sharma",
                  "gender":"FEMALE",
                  "dateOfBirth":"2014-03-15",
                  "admissionDate":"2024-06-01"
                }
                """;
        MockMultipartFile data = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "photo.jpg",
                "image/jpeg",
                new byte[] {
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46
                });
        MockMultipartFile document = new MockMultipartFile(
                "documents",
                "aadhaar.pdf",
                "application/pdf",
                "%PDF-1.4 sample".getBytes(StandardCharsets.UTF_8));

        MvcResult create = mockMvc.perform(multipart("/api/v1/students")
                        .file(data)
                        .file(photo)
                        .file(document)
                        .param("documentTypes", "AADHAAR_CARD")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(
                objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asText());
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
