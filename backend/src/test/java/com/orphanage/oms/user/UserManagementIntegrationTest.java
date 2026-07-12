package com.orphanage.oms.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import jakarta.persistence.EntityManager;
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
class UserManagementIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        var adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        User admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@oms.local")
                .passwordHash(passwordEncoder.encode("ChangeMeAdmin123!"))
                .authProvider(AuthProvider.LOCAL)
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build());
        adminUserId = admin.getId();

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
    void adminCanCreateListAndGetUser() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"nurse1",
                                  "email":"nurse1@oms.local",
                                  "role":"STAFF",
                                  "authProvider":"LOCAL",
                                  "password":"NursePass123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nurse1"))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .param("search", "nurse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("nurse1"));

        mockMvc.perform(get("/api/v1/users/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nurse1@oms.local"));
    }

    @Test
    void staffCannotAccessUserApis() throws Exception {
        String token = loginAccessToken("staff", "StaffPass123!");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"x",
                                  "email":"x@oms.local",
                                  "role":"STAFF",
                                  "authProvider":"LOCAL",
                                  "password":"Password123!"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDisableEnableAndResetPassword() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        UUID staffId = userRepository.findByUsernameIgnoreCase("staff").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/users/" + staffId + "/disable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        entityManager.clear();
        assertThat(userRepository.findById(staffId).orElseThrow().isEnabled())
                .as("enabled flag must persist as false after disable")
                .isFalse();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"staff","password":"StaffPass123!"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is disabled."));

        mockMvc.perform(post("/api/v1/users/" + staffId + "/enable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        entityManager.clear();
        assertThat(userRepository.findById(staffId).orElseThrow().isEnabled())
                .as("enabled flag must persist as true after enable")
                .isTrue();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"staff","password":"StaffPass123!"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/" + staffId + "/reset-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"BrandNewPass123!"}
                                """))
                .andExpect(status().isOk());

        entityManager.clear();
        User reloaded = userRepository.findByIdWithRole(staffId).orElseThrow();
        assertThat(passwordEncoder.matches("BrandNewPass123!", reloaded.getPasswordHash())).isTrue();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"staff","password":"BrandNewPass123!"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void deleteAliasesDisable() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        UUID staffId = userRepository.findByUsernameIgnoreCase("staff").orElseThrow().getId();

        mockMvc.perform(delete("/api/v1/users/" + staffId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        entityManager.clear();
        assertThat(userRepository.findById(staffId)).isPresent();
        assertThat(userRepository.findById(staffId).orElseThrow().isEnabled()).isFalse();
    }

    @Test
    void cannotDisableSelf() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");

        mockMvc.perform(post("/api/v1/users/" + adminUserId + "/disable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotDisableLastAdmin() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");

        mockMvc.perform(post("/api/v1/users/" + adminUserId + "/disable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserRole() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");
        UUID staffId = userRepository.findByUsernameIgnoreCase("staff").orElseThrow().getId();

        mockMvc.perform(put("/api/v1/users/" + staffId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"staff",
                                  "email":"staff@oms.local",
                                  "role":"ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void createGoogleUserWithoutPassword() throws Exception {
        String token = loginAccessToken("admin", "ChangeMeAdmin123!");

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"gstaff",
                                  "email":"gstaff@oms.local",
                                  "role":"STAFF",
                                  "authProvider":"GOOGLE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authProvider").value("GOOGLE"));
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
