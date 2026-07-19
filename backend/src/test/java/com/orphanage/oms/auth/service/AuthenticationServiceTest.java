package com.orphanage.oms.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orphanage.oms.audit.enums.AuditAction;
import com.orphanage.oms.audit.enums.AuditModule;
import com.orphanage.oms.audit.service.AuditService;
import com.orphanage.oms.auth.dto.AuthResponse;
import com.orphanage.oms.auth.dto.GoogleLoginRequest;
import com.orphanage.oms.auth.dto.LoginRequest;
import com.orphanage.oms.auth.dto.LogoutRequest;
import com.orphanage.oms.auth.dto.RefreshTokenRequest;
import com.orphanage.oms.auth.dto.UserResponse;
import com.orphanage.oms.auth.mapper.AuthMapper;
import com.orphanage.oms.config.SecurityProperties;
import com.orphanage.oms.exception.ApiException;
import com.orphanage.oms.security.UserPrincipal;
import com.orphanage.oms.security.jwt.JwtService;
import com.orphanage.oms.security.oauth.GoogleIdentity;
import com.orphanage.oms.security.oauth.GoogleTokenVerifier;
import com.orphanage.oms.user.entity.Role;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import com.orphanage.oms.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;
    @Mock
    private AuthMapper authMapper;
    @Mock
    private AuditService auditService;

    private SecurityProperties securityProperties;
    private AuthenticationService authenticationService;

    private Role staffRole;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties(
                null,
                new SecurityProperties.Google(""),
                new SecurityProperties.Lockout(5, 15),
                null);
        authenticationService = new AuthenticationService(
                authenticationManager,
                userRepository,
                jwtService,
                refreshTokenService,
                googleTokenVerifier,
                authMapper,
                securityProperties,
                auditService);

        staffRole = Role.builder()
                .id(UUID.randomUUID())
                .name(RoleName.STAFF)
                .description("Staff")
                .build();
        user = User.builder()
                .id(UUID.randomUUID())
                .username("staff1")
                .email("staff1@oms.local")
                .passwordHash("hash")
                .authProvider(AuthProvider.LOCAL)
                .role(staffRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
        userResponse = new UserResponse(user.getId(), user.getUsername(), user.getEmail(), RoleName.STAFF);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginSuccessIssuesTokensAndAudits() {
        when(userRepository.findByUsernameIgnoreCase("staff1")).thenReturn(Optional.of(user));
        UserPrincipal principal = new UserPrincipal(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtService.createAccessToken(principal)).thenReturn("access");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(3600_000L);
        when(refreshTokenService.issue(eq(user), eq("127.0.0.1"), eq("agent"))).thenReturn("refresh");
        when(authMapper.toUserResponse(user)).thenReturn(userResponse);
        when(userRepository.save(user)).thenReturn(user);

        AuthResponse response =
                authenticationService.login(new LoginRequest("staff1", "Password123!"), "127.0.0.1", "agent");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(auditService).record(
                eq(AuditModule.AUTH),
                eq(AuditAction.LOGIN),
                eq(user.getId()),
                anyString(),
                eq("staff1"),
                eq("127.0.0.1"));
    }

    @Test
    void loginUnknownUserReturns401() {
        when(userRepository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        authenticationService.login(new LoginRequest("missing", "x"), "1.1.1.1", "ua"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void loginNullPasswordHashRegistersFailedAttempt() {
        user.setPasswordHash(null);
        when(userRepository.findByUsernameIgnoreCase("staff1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        assertThatThrownBy(() ->
                        authenticationService.login(new LoginRequest("staff1", "x"), "1.1.1.1", "ua"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    void loginBadCredentialsLocksAfterMaxAttempts() {
        securityProperties = new SecurityProperties(
                null,
                new SecurityProperties.Google(""),
                new SecurityProperties.Lockout(1, 15),
                null);
        authenticationService = new AuthenticationService(
                authenticationManager,
                userRepository,
                jwtService,
                refreshTokenService,
                googleTokenVerifier,
                authMapper,
                securityProperties,
                auditService);
        when(userRepository.findByUsernameIgnoreCase("staff1")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        when(userRepository.save(user)).thenReturn(user);

        assertThatThrownBy(() ->
                        authenticationService.login(new LoginRequest("staff1", "wrong"), "1.1.1.1", "ua"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        assertThat(user.isAccountNonLocked()).isFalse();
        assertThat(user.getLockUntil()).isNotNull();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void loginDisabledExceptionReturns403() {
        when(userRepository.findByUsernameIgnoreCase("staff1")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        assertThatThrownBy(() ->
                        authenticationService.login(new LoginRequest("staff1", "Password123!"), "1.1.1.1", "ua"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void loginLockedExceptionReturns403() {
        when(userRepository.findByUsernameIgnoreCase("staff1")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("locked"));

        assertThatThrownBy(() ->
                        authenticationService.login(new LoginRequest("staff1", "Password123!"), "1.1.1.1", "ua"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void loginCurrentlyLockedReturns403() {
        user.setAccountNonLocked(false);
        user.setLockUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(userRepository.findByUsernameIgnoreCase("staff1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                        authenticationService.login(new LoginRequest("staff1", "Password123!"), "1.1.1.1", "ua"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void loginExpiredLockAutoUnlocksThenAuthenticates() {
        user.setAccountNonLocked(false);
        user.setLockUntil(Instant.now().minus(1, ChronoUnit.MINUTES));
        user.setFailedLoginAttempts(3);
        when(userRepository.findByUsernameIgnoreCase("staff1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        UserPrincipal principal = new UserPrincipal(user);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtService.createAccessToken(principal)).thenReturn("access");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(3600_000L);
        when(refreshTokenService.issue(any(), any(), any())).thenReturn("refresh");
        when(authMapper.toUserResponse(user)).thenReturn(userResponse);

        AuthResponse response =
                authenticationService.login(new LoginRequest("staff1", "Password123!"), "1.1.1.1", "ua");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.getLockUntil()).isNull();
    }

    @Test
    void loginWithGoogleLinksLocalUserAndIssuesTokens() {
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleIdentity("sub-1", "staff1@oms.local", true));
        when(userRepository.findByEmailIgnoreCase("staff1@oms.local")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(jwtService.createAccessToken(any(UserPrincipal.class))).thenReturn("access");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(3600_000L);
        when(refreshTokenService.issue(eq(user), eq("1.1.1.1"), eq("ua"))).thenReturn("refresh");
        when(authMapper.toUserResponse(user)).thenReturn(userResponse);

        AuthResponse response =
                authenticationService.loginWithGoogle(new GoogleLoginRequest("id-token"), "1.1.1.1", "ua");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(user.getProviderSubject()).isEqualTo("sub-1");
        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.LOCAL_GOOGLE);
        verify(auditService).record(
                eq(AuditModule.AUTH),
                eq(AuditAction.LOGIN),
                eq(user.getId()),
                anyString(),
                eq("staff1"),
                eq("1.1.1.1"));
    }

    @Test
    void loginWithGoogleSubjectMismatchReturns401() {
        user.setProviderSubject("other-sub");
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleIdentity("sub-1", "staff1@oms.local", true));
        when(userRepository.findByEmailIgnoreCase("staff1@oms.local")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                        authenticationService.loginWithGoogle(new GoogleLoginRequest("id-token"), "1.1.1.1", "ua"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refreshReturnsNewTokenPair() {
        when(refreshTokenService.rotate("old-refresh", "1.1.1.1", "ua"))
                .thenReturn(new RefreshTokenService.RotatedRefreshToken(user, "new-refresh"));
        when(jwtService.createAccessToken(any(UserPrincipal.class))).thenReturn("access");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(3600_000L);
        when(authMapper.toUserResponse(user)).thenReturn(userResponse);

        AuthResponse response =
                authenticationService.refresh(new RefreshTokenRequest("old-refresh"), "1.1.1.1", "ua");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void logoutWithPrincipalAndRefreshTokenRevokesOne() {
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        authenticationService.logout(new LogoutRequest("refresh-1"), "1.1.1.1");

        verify(refreshTokenService).revoke("refresh-1");
        verify(refreshTokenService, never()).revokeAllForUser(any());
        verify(auditService).record(
                eq(AuditModule.AUTH),
                eq(AuditAction.LOGOUT),
                eq(user.getId()),
                anyString(),
                eq("staff1"),
                eq("1.1.1.1"));
    }

    @Test
    void logoutWithPrincipalWithoutRefreshTokenRevokesAll() {
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        authenticationService.logout(new LogoutRequest(null), "1.1.1.1");

        verify(refreshTokenService).revokeAllForUser(user.getId());
        verify(refreshTokenService, never()).revoke(anyString());
    }

    @Test
    void logoutAnonymousWithRefreshTokenRevokesOnly() {
        authenticationService.logout(new LogoutRequest("orphan-refresh"), "1.1.1.1");

        verify(refreshTokenService).revoke("orphan-refresh");
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void meReturnsCurrentUser() {
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(userRepository.findByIdWithRole(user.getId())).thenReturn(Optional.of(user));
        when(authMapper.toUserResponse(user)).thenReturn(userResponse);

        assertThat(authenticationService.me()).isEqualTo(userResponse);
    }
}
