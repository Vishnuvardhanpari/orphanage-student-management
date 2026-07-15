package com.orphanage.oms.user.controller;

import com.orphanage.oms.common.dto.PageResponse;
import com.orphanage.oms.user.dto.CreateUserRequest;
import com.orphanage.oms.user.dto.ResetPasswordRequest;
import com.orphanage.oms.user.dto.UpdateUserRequest;
import com.orphanage.oms.user.dto.UserDetailResponse;
import com.orphanage.oms.user.enums.RoleName;
import com.orphanage.oms.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only user management endpoints.
 */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users with pagination, search, and filters")
    public PageResponse<UserDetailResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(userService.list(search, role, enabled, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by id")
    public UserDetailResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a pre-provisioned user")
    public UserDetailResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile and role")
    public UserDetailResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Disable a user (soft disable, not hard delete)")
    public UserDetailResponse delete(@PathVariable UUID id) {
        return userService.disable(id);
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "Disable a user and revoke refresh tokens")
    public UserDetailResponse disable(@PathVariable UUID id) {
        return userService.disable(id);
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "Enable a user and clear lockout")
    public UserDetailResponse enable(@PathVariable UUID id) {
        return userService.enable(id);
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Admin reset of a user's password")
    public ResponseEntity<UserDetailResponse> resetPassword(
            @PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userService.resetPassword(id, request));
    }
}
