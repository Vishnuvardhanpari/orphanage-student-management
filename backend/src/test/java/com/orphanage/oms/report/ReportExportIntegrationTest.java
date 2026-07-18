package com.orphanage.oms.report;

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
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
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
class ReportExportIntegrationTest {

    private static final byte[] PNG_1X1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

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
    void staffCanExportSingleStudentPdf() throws Exception {
        String token = loginAccessToken("staff", "StaffPass123!");
        UUID studentId = registerStudent(token, "ADM-R-100", "Anita", true);

        MvcResult result = mockMvc.perform(get("/api/v1/reports/student/{studentId}", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        Matchers.containsString("student-report-ADM-R-100.pdf")))
                .andReturn();

        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void adminCanExportSelectedStudents() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        UUID id1 = registerStudent(token, "ADM-R-201", "Ravi", false);
        UUID id2 = registerStudent(token, "ADM-R-202", "Neha", false);

        MvcResult result = mockMvc.perform(post("/api/v1/reports/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("studentIds", List.of(id1, id2)))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        Matchers.containsString("students-report-")))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void staffCanExportFilteredStudents() throws Exception {
        String token = loginAccessToken("staff", "StaffPass123!");
        registerStudent(token, "ADM-R-301", "FilteredOne", false);
        registerStudent(token, "ADM-R-302", "Other", false);

        MvcResult result = mockMvc.perform(post("/api/v1/reports/filter")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope":"ACTIVE","search":"FilteredOne","gender":"FEMALE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void staffCanExportArchivedStudentsByFilterScope() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        UUID studentId = registerStudent(token, "ADM-R-350", "LeftOne", false);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/reports/filter")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope":"ARCHIVED","search":"LeftOne"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    void filterWithNoMatchesReturns400() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        mockMvc.perform(post("/api/v1/reports/filter")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"search":"definitely-no-match-xyz"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No students match the selected filters."));
    }

    @Test
    void unknownStudentReturns404() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        mockMvc.perform(get("/api/v1/reports/student/{studentId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student not found."));
    }

    @Test
    void emptySelectionReturns400() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        mockMvc.perform(post("/api/v1/reports/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentIds":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedCannotExport() throws Exception {
        mockMvc.perform(get("/api/v1/reports/student/{studentId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void archivedStudentCanBeExportedIndividually() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        UUID studentId = registerStudent(token, "ADM-R-400", "Archived", false);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/reports/student/{studentId}", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE));
    }

    private UUID registerStudent(String token, String admissionNumber, String firstName, boolean withFiles)
            throws Exception {
        String json = """
                {
                  "admissionNumber":"%s",
                  "firstName":"%s",
                  "lastName":"Test",
                  "gender":"FEMALE",
                  "dateOfBirth":"2014-03-15",
                  "admissionDate":"2024-06-01"
                }
                """.formatted(admissionNumber, firstName);
        MockMultipartFile data = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));

        org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder builder =
                multipart("/api/v1/students").file(data);
        if (withFiles) {
            MockMultipartFile photo = new MockMultipartFile(
                    "photo", "photo.png", "image/png", PNG_1X1);
            MockMultipartFile document = new MockMultipartFile(
                    "documents",
                    "aadhaar.pdf",
                    "application/pdf",
                    "%PDF-1.4 sample".getBytes(StandardCharsets.UTF_8));
            MockMultipartFile imageDoc = new MockMultipartFile(
                    "documents",
                    "mark.png",
                    "image/png",
                    PNG_1X1);
            builder.file(photo)
                    .file(document)
                    .file(imageDoc)
                    .param("documentTypes", "AADHAAR_CARD", "MARK_SHEET");
        }

        MvcResult create = mockMvc.perform(builder.header("Authorization", "Bearer " + token))
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
