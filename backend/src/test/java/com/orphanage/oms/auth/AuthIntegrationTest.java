package com.orphanage.oms.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orphanage.oms.auth.repository.RefreshTokenRepository;
import com.orphanage.oms.security.oauth.GoogleIdentity;
import com.orphanage.oms.security.oauth.GoogleTokenVerifier;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import com.orphanage.oms.user.repository.RoleRepository;
import com.orphanage.oms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

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
    }

    @Test
    void loginReturnsTokens() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"ChangeMeAdmin123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void loginWithBadPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsCurrentUserWithBearerToken() throws Exception {
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void refreshRotatesTokensAndOldRefreshFails() throws Exception {
        JsonNode login = loginPayload();
        String refreshToken = login.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String newRefresh = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("refreshToken")
                .asText();
        assertThat(newRefresh).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        JsonNode login = loginPayload();
        String accessToken = login.get("accessToken").asText();
        String refreshToken = login.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleLoginForProvisionedUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"test-google-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("admin@oms.local"));
    }

    private String loginAndGetAccessToken() throws Exception {
        return loginPayload().get("accessToken").asText();
    }

    private JsonNode loginPayload() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"ChangeMeAdmin123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @TestConfiguration
    static class GoogleVerifierTestConfig {

        @Bean
        @Primary
        GoogleTokenVerifier googleTokenVerifier() {
            return idToken -> new GoogleIdentity("google-sub-1", "admin@oms.local", true);
        }
    }
}
