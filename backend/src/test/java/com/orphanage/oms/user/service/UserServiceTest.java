package com.orphanage.oms.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orphanage.oms.auth.service.RefreshTokenService;
import com.orphanage.oms.exception.ApiException;
import com.orphanage.oms.security.UserPrincipal;
import com.orphanage.oms.user.dto.CreateUserRequest;
import com.orphanage.oms.user.dto.ResetPasswordRequest;
import com.orphanage.oms.user.dto.UpdateUserRequest;
import com.orphanage.oms.user.dto.UserDetailResponse;
import com.orphanage.oms.user.entity.Role;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import com.orphanage.oms.user.mapper.UserMapper;
import com.orphanage.oms.user.repository.RoleRepository;
import com.orphanage.oms.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    private UUID adminId;
    private Role adminRole;
    private Role staffRole;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        adminRole = Role.builder().id(UUID.randomUUID()).name(RoleName.ADMIN).description("Admin").build();
        staffRole = Role.builder().id(UUID.randomUUID()).name(RoleName.STAFF).description("Staff").build();

        User actor = User.builder()
                .id(adminId)
                .username("admin")
                .email("admin@oms.local")
                .passwordHash("hash")
                .authProvider(AuthProvider.LOCAL)
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
        UserPrincipal principal = new UserPrincipal(actor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createLocalUserEncodesPassword() {
        CreateUserRequest request = new CreateUserRequest(
                "staff1", "staff1@oms.local", RoleName.STAFF, AuthProvider.LOCAL, "Password123!");
        when(userRepository.existsByUsernameIgnoreCase("staff1")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("staff1@oms.local")).thenReturn(false);
        when(roleRepository.findByName(RoleName.STAFF)).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toDetailResponse(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new UserDetailResponse(
                    u.getId(), u.getUsername(), u.getEmail(), RoleName.STAFF, true,
                    AuthProvider.LOCAL, true, null, null, null);
        });

        UserDetailResponse response = userService.create(request);

        assertThat(response.username()).isEqualTo("staff1");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded");
        assertThat(captor.getValue().getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
    }

    @Test
    void createLocalUserWithoutPasswordFails() {
        CreateUserRequest request = new CreateUserRequest(
                "staff1", "staff1@oms.local", RoleName.STAFF, AuthProvider.LOCAL, null);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Password is required");
    }

    @Test
    void disableSelfFails() {
        User self = User.builder()
                .id(adminId)
                .username("admin")
                .email("admin@oms.local")
                .passwordHash("hash")
                .authProvider(AuthProvider.LOCAL)
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
        when(userRepository.findByIdWithRole(adminId)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> userService.disable(adminId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("your own account");
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void disableLastAdminFails() {
        UUID otherAdminId = UUID.randomUUID();
        User other = User.builder()
                .id(otherAdminId)
                .username("admin2")
                .email("admin2@oms.local")
                .passwordHash("hash")
                .authProvider(AuthProvider.LOCAL)
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
        when(userRepository.findByIdWithRole(otherAdminId)).thenReturn(Optional.of(other));
        when(userRepository.countEnabledAdmins()).thenReturn(1L);

        assertThatThrownBy(() -> userService.disable(otherAdminId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("last enabled administrator");
    }

    @Test
    void resetPasswordUpgradesGoogleProvider() {
        UUID userId = UUID.randomUUID();
        User googleUser = User.builder()
                .id(userId)
                .username("guser")
                .email("guser@oms.local")
                .passwordHash(null)
                .authProvider(AuthProvider.GOOGLE)
                .role(staffRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(2)
                .build();
        when(userRepository.findByIdWithRole(userId)).thenReturn(Optional.of(googleUser));
        when(passwordEncoder.encode("NewPass123!")).thenReturn("new-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toDetailResponse(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new UserDetailResponse(
                    u.getId(), u.getUsername(), u.getEmail(), RoleName.STAFF, true,
                    u.getAuthProvider(), true, null, null, null);
        });

        UserDetailResponse response = userService.resetPassword(userId, new ResetPasswordRequest("NewPass123!"));

        assertThat(response.authProvider()).isEqualTo(AuthProvider.LOCAL_GOOGLE);
        verify(refreshTokenService).revokeAllForUser(eq(userId));
    }

    @Test
    void demoteLastAdminFails() {
        UUID otherAdminId = UUID.randomUUID();
        User other = User.builder()
                .id(otherAdminId)
                .username("admin2")
                .email("admin2@oms.local")
                .passwordHash("hash")
                .authProvider(AuthProvider.LOCAL)
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
        when(userRepository.findByIdWithRole(otherAdminId)).thenReturn(Optional.of(other));
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("admin2", otherAdminId)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("admin2@oms.local", otherAdminId)).thenReturn(false);
        when(userRepository.countEnabledAdmins()).thenReturn(1L);

        UpdateUserRequest request = new UpdateUserRequest("admin2", "admin2@oms.local", RoleName.STAFF);

        assertThatThrownBy(() -> userService.update(otherAdminId, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("last enabled administrator");
    }
}
