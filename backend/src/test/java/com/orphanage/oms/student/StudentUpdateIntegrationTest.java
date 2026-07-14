package com.orphanage.oms.student;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class StudentUpdateIntegrationTest {

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
        // Native deletes bypass @SQLRestriction so soft-deleted documents are purged too.
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
    void staffCanUpdateFieldsReplacePhotoAddAndReplaceDocuments() throws Exception {
        String token = loginAccessToken("staff", "StaffPass123!");
        UUID studentId = registerStudent(token, "ADM-U-100", null);

        mockMvc.perform(put("/api/v1/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Anita",
                                  "lastName":"Updated",
                                  "gender":"FEMALE",
                                  "dateOfBirth":"2014-03-15",
                                  "aadhaarNumber":"123456789012",
                                  "guardianName":"Guardian",
                                  "schoolName":"New School",
                                  "admissionDate":"2024-06-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anita"))
                .andExpect(jsonPath("$.lastName").value("Updated"))
                .andExpect(jsonPath("$.aadhaarNumber").value("123456789012"))
                .andExpect(jsonPath("$.schoolName").value("New School"))
                .andExpect(jsonPath("$.admissionNumber").value("ADM-U-100"))
                .andExpect(jsonPath("$.profilePhotoPath").doesNotExist());

        MockMultipartFile photo = jpegPhoto("photo");
        mockMvc.perform(multipart("/api/v1/students/{id}/photo", studentId)
                        .file(photo)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasProfilePhoto").value(true));

        MockMultipartFile extraDoc = new MockMultipartFile(
                "documents",
                "birth.pdf",
                "application/pdf",
                "%PDF-1.4 birth".getBytes(StandardCharsets.UTF_8));
        MvcResult addDocs = mockMvc.perform(multipart("/api/v1/students/{id}/documents", studentId)
                        .file(extraDoc)
                        .param("documentTypes", "BIRTH_CERTIFICATE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].documentType").value("BIRTH_CERTIFICATE"))
                .andExpect(jsonPath("$[0].originalFileName").value("birth.pdf"))
                .andReturn();

        UUID documentId = UUID.fromString(
                objectMapper.readTree(addDocs.getResponse().getContentAsString()).get(0).get("id").asText());

        MockMultipartFile replacement = new MockMultipartFile(
                "document",
                "identity.pdf",
                "application/pdf",
                "%PDF-1.4 identity".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/students/{id}/documents/{documentId}", studentId, documentId)
                        .file(replacement)
                        .param("documentType", "IDENTITY_PROOF")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("IDENTITY_PROOF"))
                .andExpect(jsonPath("$.originalFileName").value("identity.pdf"));

        mockMvc.perform(get("/api/v1/students/{id}/documents", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].documentType").value("IDENTITY_PROOF"));
    }

    @Test
    void staffCanDeletePhotoAndSoftDeleteDocuments() throws Exception {
        String token = loginAccessToken("staff", "StaffPass123!");
        UUID studentId = registerStudent(token, "ADM-U-300", null);

        // Photo delete without a photo on file → 404.
        mockMvc.perform(delete("/api/v1/students/{id}/photo", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Profile photo not found."));

        MockMultipartFile photo = jpegPhoto("photo");
        mockMvc.perform(multipart("/api/v1/students/{id}/photo", studentId)
                        .file(photo)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/students/{id}/photo", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasProfilePhoto").value(false));

        mockMvc.perform(get("/api/v1/students/{id}/photo", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        MockMultipartFile document = new MockMultipartFile(
                "documents",
                "aadhaar.pdf",
                "application/pdf",
                "%PDF-1.4 aadhaar".getBytes(StandardCharsets.UTF_8));
        MvcResult addDocs = mockMvc.perform(multipart("/api/v1/students/{id}/documents", studentId)
                        .file(document)
                        .param("documentTypes", "AADHAAR_CARD")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        UUID documentId = UUID.fromString(
                objectMapper.readTree(addDocs.getResponse().getContentAsString()).get(0).get("id").asText());

        mockMvc.perform(delete("/api/v1/students/{id}/documents/{documentId}", studentId, documentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Soft-deleted document is hidden from list, download, and repeat delete.
        mockMvc.perform(get("/api/v1/students/{id}/documents", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/students/{id}/documents/{documentId}/download", studentId, documentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/students/{id}/documents/{documentId}", studentId, documentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Document not found."));
    }

    @Test
    void deleteDocumentRejectsForeignStudent() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        UUID ownerId = registerStudent(token, "ADM-U-310", null);
        UUID otherId = registerStudent(token, "ADM-U-311", null);

        MockMultipartFile document = new MockMultipartFile(
                "documents",
                "birth.pdf",
                "application/pdf",
                "%PDF-1.4 birth".getBytes(StandardCharsets.UTF_8));
        MvcResult addDocs = mockMvc.perform(multipart("/api/v1/students/{id}/documents", ownerId)
                        .file(document)
                        .param("documentTypes", "BIRTH_CERTIFICATE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        UUID documentId = UUID.fromString(
                objectMapper.readTree(addDocs.getResponse().getContentAsString()).get(0).get("id").asText());

        mockMvc.perform(delete("/api/v1/students/{id}/documents/{documentId}", otherId, documentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedCannotDeleteMedia() throws Exception {
        mockMvc.perform(delete("/api/v1/students/{id}/photo", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(
                        "/api/v1/students/{id}/documents/{documentId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateRejectsDuplicateAadhaarFromAnotherStudent() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        registerStudent(token, "ADM-U-200", "999999999999");
        UUID secondId = registerStudent(token, "ADM-U-201", null);

        mockMvc.perform(put("/api/v1/students/{id}", secondId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Ravi",
                                  "gender":"MALE",
                                  "dateOfBirth":"2015-05-01",
                                  "aadhaarNumber":"999999999999",
                                  "admissionDate":"2024-06-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Aadhaar number is already in use."));
    }

    @Test
    void updateUnknownStudentReturns404() throws Exception {
        String token = loginAccessToken("staff", "StaffPass123!");
        mockMvc.perform(put("/api/v1/students/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Ravi",
                                  "gender":"MALE",
                                  "dateOfBirth":"2015-05-01",
                                  "admissionDate":"2024-06-01"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student not found."));
    }

    @Test
    void unauthenticatedCannotUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/students/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Ravi",
                                  "gender":"MALE",
                                  "dateOfBirth":"2015-05-01",
                                  "admissionDate":"2024-06-01"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private UUID registerStudent(String token, String admissionNumber, String aadhaar) throws Exception {
        String aadhaarJson = aadhaar == null ? "" : ",\"aadhaarNumber\":\"" + aadhaar + "\"";
        String json = """
                {
                  "admissionNumber":"%s",
                  "firstName":"Anita",
                  "gender":"FEMALE",
                  "dateOfBirth":"2014-03-15",
                  "admissionDate":"2024-06-01"%s
                }
                """.formatted(admissionNumber, aadhaarJson);
        MockMultipartFile data = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));

        MvcResult create = mockMvc.perform(multipart("/api/v1/students")
                        .file(data)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(
                objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asText());
    }

    private static MockMultipartFile jpegPhoto(String partName) {
        return new MockMultipartFile(
                partName,
                "photo.jpg",
                "image/jpeg",
                new byte[] {
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46
                });
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
