package com.orphanage.oms.security.jwt;

import com.orphanage.oms.config.SecurityProperties;
import com.orphanage.oms.security.UserPrincipal;
import com.orphanage.oms.user.entity.Role;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Jwt(
                        "test-secret-key-must-be-at-least-32-characters-long",
                        3_600_000L,
                        604_800_000L,
                        "oms-test"),
                new SecurityProperties.Google("client"),
                new SecurityProperties.Lockout(5, 15),
                new SecurityProperties.Bootstrap("admin", "admin@oms.local", "ChangeMeAdmin123!"));
        jwtService = new JwtService(properties);
    }

    @Test
    void createAndParseAccessToken() {
        UserPrincipal principal = principal("admin", RoleName.ADMIN);
        String token = jwtService.createAccessToken(principal);

        Claims claims = jwtService.parseClaims(token);

        assertThat(jwtService.extractUserId(claims)).isEqualTo(principal.getId());
        assertThat(jwtService.extractUsername(claims)).isEqualTo("admin");
        assertThat(jwtService.extractRole(claims)).isEqualTo(RoleName.ADMIN);
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void rejectsTamperedToken() {
        UserPrincipal principal = principal("admin", RoleName.ADMIN);
        String token = jwtService.createAccessToken(principal);
        String tampered = token.substring(0, token.length() - 4) + "aaaa";

        assertThat(jwtService.isValid(tampered)).isFalse();
        assertThatThrownBy(() -> jwtService.parseClaims(tampered)).isInstanceOf(RuntimeException.class);
    }

    private UserPrincipal principal(String username, RoleName roleName) {
        Role role = Role.builder()
                .id(UUID.randomUUID())
                .name(roleName)
                .description(roleName.name())
                .createdDate(Instant.now())
                .build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(username + "@oms.local")
                .passwordHash("hash")
                .authProvider(AuthProvider.LOCAL)
                .role(role)
                .enabled(true)
                .accountNonLocked(true)
                .build();
        return new UserPrincipal(user);
    }
}
