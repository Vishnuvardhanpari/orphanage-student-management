package com.orphanage.oms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orphanage.oms.exception.ApiException;
import com.orphanage.oms.user.entity.Role;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentPrincipalEmptyWhenUnauthenticated() {
        assertThat(SecurityUtils.currentPrincipal()).isEmpty();
        assertThat(SecurityUtils.currentUsername()).isEmpty();
    }

    @Test
    void requirePrincipalThrowsWhenMissing() {
        assertThatThrownBy(SecurityUtils::requirePrincipal)
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void requireHelpersReturnPrincipalFields() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@oms.local")
                .passwordHash("hash")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.builder().id(UUID.randomUUID()).name(RoleName.ADMIN).description("Admin").build())
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertThat(SecurityUtils.requirePrincipal()).isEqualTo(principal);
        assertThat(SecurityUtils.requireUserId()).isEqualTo(user.getId());
        assertThat(SecurityUtils.requireUsername()).isEqualTo("admin");
        assertThat(SecurityUtils.currentUsername()).contains("admin");
    }
}
