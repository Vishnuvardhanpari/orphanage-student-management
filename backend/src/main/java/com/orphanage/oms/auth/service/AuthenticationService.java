package com.orphanage.oms.auth.service;

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
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Authentication use-cases: login, Google login, refresh, logout, and current user.
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final String INVALID_CREDENTIALS = "Invalid username or password.";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AuthMapper authMapper;
    private final SecurityProperties securityProperties;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            GoogleTokenVerifier googleTokenVerifier,
            AuthMapper authMapper,
            SecurityProperties securityProperties) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.authMapper = authMapper;
        this.securityProperties = securityProperties;
    }

    /**
     * Authenticates with username and password.
     *
     * @param request   login request
     * @param clientIp  client IP
     * @param userAgent user agent
     * @return auth response
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp, String userAgent) {
        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> unauthorized(INVALID_CREDENTIALS));

        assertNotLocked(user);

        if (user.getPasswordHash() == null) {
            registerFailedAttempt(user);
            throw unauthorized(INVALID_CREDENTIALS);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            clearFailedAttempts(user);
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            log.info("User '{}' logged in via password", user.getUsername());
            return issueTokens(principal, user, clientIp, userAgent);
        } catch (BadCredentialsException ex) {
            registerFailedAttempt(user);
            throw unauthorized(INVALID_CREDENTIALS);
        } catch (DisabledException ex) {
            throw forbidden("Account is disabled.");
        } catch (LockedException ex) {
            throw forbidden("Account is locked. Try again later.");
        }
    }

    /**
     * Authenticates with a Google ID token for a pre-provisioned user.
     *
     * @param request   Google login request
     * @param clientIp  client IP
     * @param userAgent user agent
     * @return auth response
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request, String clientIp, String userAgent) {
        GoogleIdentity identity = googleTokenVerifier.verify(request.idToken());

        User user = userRepository.findByEmailIgnoreCase(identity.email())
                .orElseThrow(() -> unauthorized("Invalid username or password."));

        assertNotLocked(user);
        if (!user.isEnabled()) {
            throw forbidden("Account is disabled.");
        }

        if (user.getProviderSubject() == null) {
            user.setProviderSubject(identity.subject());
            if (user.getAuthProvider() == AuthProvider.LOCAL) {
                user.setAuthProvider(AuthProvider.LOCAL_GOOGLE);
            } else if (user.getAuthProvider() == null) {
                user.setAuthProvider(AuthProvider.GOOGLE);
            }
        } else if (!user.getProviderSubject().equals(identity.subject())) {
            throw unauthorized("Invalid username or password.");
        }

        clearFailedAttempts(user);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        log.info("User '{}' logged in via Google", user.getUsername());
        return issueTokens(principal, user, clientIp, userAgent);
    }

    /**
     * Rotates refresh token and returns a new access token pair.
     *
     * @param request   refresh request
     * @param clientIp  client IP
     * @param userAgent user agent
     * @return auth response
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String clientIp, String userAgent) {
        RefreshTokenService.RotatedRefreshToken rotated =
                refreshTokenService.rotate(request.refreshToken(), clientIp, userAgent);
        User user = rotated.user();
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.createAccessToken(principal);
        return new AuthResponse(
                accessToken,
                rotated.rawRefreshToken(),
                "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000,
                authMapper.toUserResponse(user));
    }

    /**
     * Revokes refresh token(s) for logout.
     *
     * @param request logout request
     */
    @Transactional
    public void logout(LogoutRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal principal) {
            if (StringUtils.hasText(request != null ? request.refreshToken() : null)) {
                refreshTokenService.revoke(request.refreshToken());
            } else {
                refreshTokenService.revokeAllForUser(principal.getId());
            }
            log.info("User '{}' logged out", principal.getUsername());
            return;
        }

        if (request != null && StringUtils.hasText(request.refreshToken())) {
            refreshTokenService.revoke(request.refreshToken());
            log.info("Refresh token revoked on logout");
            return;
        }

        // Idempotent no-op when nothing to revoke
    }

    /**
     * Returns the currently authenticated user profile.
     *
     * @return user response
     */
    @Transactional(readOnly = true)
    public UserResponse me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw unauthorized("Authentication is required.");
        }
        User user = userRepository.findByIdWithRole(principal.getId())
                .orElseThrow(() -> unauthorized("Authentication is required."));
        return authMapper.toUserResponse(user);
    }

    private AuthResponse issueTokens(UserPrincipal principal, User user, String clientIp, String userAgent) {
        String accessToken = jwtService.createAccessToken(principal);
        String refreshToken = refreshTokenService.issue(user, clientIp, userAgent);
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000,
                authMapper.toUserResponse(user));
    }

    private void assertNotLocked(User user) {
        if (!user.isEnabled()) {
            throw forbidden("Account is disabled.");
        }
        if (user.getLockUntil() != null && Instant.now().isAfter(user.getLockUntil())) {
            user.setAccountNonLocked(true);
            user.setLockUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            return;
        }
        if (user.isCurrentlyLocked() || !user.isAccountNonLocked()) {
            throw forbidden("Account is locked. Try again later.");
        }
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        int max = securityProperties.lockout().maxFailedAttempts();
        if (attempts >= max) {
            long minutes = securityProperties.lockout().durationMinutes();
            user.setLockUntil(Instant.now().plus(minutes, ChronoUnit.MINUTES));
            user.setAccountNonLocked(false);
            user.setFailedLoginAttempts(0);
            log.warn("Account '{}' locked after failed login attempts", user.getUsername());
        }
        userRepository.save(user);
    }

    private void clearFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);
        user.setAccountNonLocked(true);
    }

    private ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", message);
    }

    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "Forbidden", message);
    }
}
