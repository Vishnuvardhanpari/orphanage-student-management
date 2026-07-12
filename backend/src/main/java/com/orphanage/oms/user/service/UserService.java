package com.orphanage.oms.user.service;

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
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin user management: create, update, list, enable/disable, and reset password.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public Page<UserDetailResponse> list(String search, RoleName role, Boolean enabled, Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return userRepository.search(normalizedSearch, role, enabled, pageable).map(userMapper::toDetailResponse);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getById(UUID id) {
        return userMapper.toDetailResponse(requireUser(id));
    }

    @Transactional
    public UserDetailResponse create(CreateUserRequest request) {
        AuthProvider provider = request.authProvider();
        if (provider != AuthProvider.LOCAL && provider != AuthProvider.GOOGLE) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "authProvider must be LOCAL or GOOGLE when creating a user.");
        }

        if (provider == AuthProvider.LOCAL
                && (request.password() == null || request.password().isBlank())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Password is required for LOCAL users.");
        }

        if (provider == AuthProvider.GOOGLE
                && request.password() != null
                && !request.password().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Error",
                    "Password must be omitted for GOOGLE users; use reset-password later if needed.");
        }

        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Conflict", "Username is already in use.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Conflict", "Email is already in use.");
        }

        Role role = requireRole(request.role());
        UUID actorId = currentUserId();

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(provider == AuthProvider.LOCAL
                        ? passwordEncoder.encode(request.password())
                        : null)
                .authProvider(provider)
                .role(role)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        User saved = userRepository.save(user);
        log.info("User created id={} username={} role={} by={}",
                saved.getId(), saved.getUsername(), saved.getRole().getName(), actorId);
        return userMapper.toDetailResponse(saved);
    }

    @Transactional
    public UserDetailResponse update(UUID id, UpdateUserRequest request) {
        User user = requireUser(id);
        UUID actorId = currentUserId();

        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Conflict", "Username is already in use.");
        }
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Conflict", "Email is already in use.");
        }

        RoleName newRole = request.role();
        RoleName currentRole = user.getRole().getName();
        if (currentRole == RoleName.ADMIN && newRole != RoleName.ADMIN) {
            assertNotSelf(id, "You cannot change your own role.");
            assertNotLastEnabledAdmin(user, "Cannot demote the last enabled administrator.");
        }

        user.setUsername(username);
        user.setEmail(email);
        user.setRole(requireRole(newRole));
        user.setUpdatedBy(actorId);

        User saved = userRepository.save(user);
        log.info("User updated id={} by={}", saved.getId(), actorId);
        return userMapper.toDetailResponse(saved);
    }

    @Transactional
    public UserDetailResponse disable(UUID id) {
        User user = requireUser(id);
        assertNotSelf(id, "You cannot disable your own account.");
        if (user.getRole().getName() == RoleName.ADMIN && user.isEnabled()) {
            assertNotLastEnabledAdmin(user, "Cannot disable the last enabled administrator.");
        }

        if (!user.isEnabled()) {
            return userMapper.toDetailResponse(user);
        }

        user.setEnabled(false);
        user.setUpdatedBy(currentUserId());
        User saved = userRepository.saveAndFlush(user);
        refreshTokenService.revokeAllForUser(id);
        log.info("User disabled id={} by={}", id, currentUserId());
        return userMapper.toDetailResponse(saved);
    }

    @Transactional
    public UserDetailResponse enable(UUID id) {
        User user = requireUser(id);
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);
        user.setUpdatedBy(currentUserId());
        User saved = userRepository.save(user);
        log.info("User enabled id={} by={}", id, currentUserId());
        return userMapper.toDetailResponse(saved);
    }

    @Transactional
    public UserDetailResponse resetPassword(UUID id, ResetPasswordRequest request) {
        User user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            user.setAuthProvider(AuthProvider.LOCAL_GOOGLE);
        } else if (user.getAuthProvider() == null) {
            user.setAuthProvider(AuthProvider.LOCAL);
        }
        user.setAccountNonLocked(true);
        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);
        user.setUpdatedBy(currentUserId());
        User saved = userRepository.saveAndFlush(user);
        refreshTokenService.revokeAllForUser(id);
        log.info("Password reset for user id={} by={}", id, currentUserId());
        return userMapper.toDetailResponse(saved);
    }

    private User requireUser(UUID id) {
        return userRepository.findByIdWithRole(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Not Found", "User not found."));
    }

    private Role requireRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST, "Validation Error", "Role does not exist: " + roleName));
    }

    private void assertNotSelf(UUID targetId, String message) {
        if (targetId.equals(currentUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validation Error", message);
        }
    }

    private void assertNotLastEnabledAdmin(User user, String message) {
        if (user.getRole().getName() != RoleName.ADMIN || !user.isEnabled()) {
            return;
        }
        if (userRepository.countEnabledAdmins() <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validation Error", message);
        }
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication required.");
        }
        return principal.getId();
    }
}
