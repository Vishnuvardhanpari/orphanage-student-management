package com.orphanage.oms.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class DisabledUserLoginIT {

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

    @BeforeEach
    void setUp() {
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
    void disabledUserCannotLoginAndEnabledPersistsFalse() throws Exception {
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
        assertThat(userRepository.findById(staffId).orElseThrow().isEnabled()).isTrue();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"staff","password":"StaffPass123!"}
                                """))
                .andExpect(status().isOk());
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
