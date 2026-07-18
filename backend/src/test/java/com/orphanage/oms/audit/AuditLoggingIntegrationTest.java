package com.orphanage.oms.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orphanage.oms.audit.enums.AuditAction;
import com.orphanage.oms.audit.enums.AuditModule;
import com.orphanage.oms.audit.repository.AuditLogRepository;
import com.orphanage.oms.auth.repository.RefreshTokenRepository;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import com.orphanage.oms.user.repository.RoleRepository;
import com.orphanage.oms.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLoggingIntegrationTest {

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
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        auditLogRepository.deleteAll();
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
    void loginCreatesAuditRecordAndAdminCanListAndGet() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getModule() == AuditModule.AUTH
                        && log.getAction() == AuditAction.LOGIN
                        && "admin".equals(log.getUsername()));

        MvcResult listResult = mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", "Bearer " + token)
                        .param("module", "AUTH")
                        .param("action", "LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].module").value("AUTH"))
                .andExpect(jsonPath("$.content[0].action").value("LOGIN"))
                .andExpect(jsonPath("$.content[0].username").value("admin"))
                .andReturn();

        String id = objectMapper
                .readTree(listResult.getResponse().getContentAsString())
                .get("content")
                .get(0)
                .get("id")
                .asText();

        mockMvc.perform(get("/api/v1/audit/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("User logged in via password"));
    }

    @Test
    void staffCannotAccessAuditApis() throws Exception {
        String token = loginAccessToken("staff", "StaffPass123!");

        mockMvc.perform(get("/api/v1/audit").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/audit/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutCreatesAuditRecord() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getModule() == AuditModule.AUTH
                        && log.getAction() == AuditAction.LOGOUT
                        && "admin".equals(log.getUsername()));
    }

    @Test
    void unknownAuditIdReturns404() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");

        mockMvc.perform(get("/api/v1/audit/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String loginAccessToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }
}
