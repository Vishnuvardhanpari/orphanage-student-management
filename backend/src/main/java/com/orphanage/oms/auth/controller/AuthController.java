package com.orphanage.oms.auth.controller;

import com.orphanage.oms.auth.dto.AuthResponse;
import com.orphanage.oms.auth.dto.GoogleLoginRequest;
import com.orphanage.oms.auth.dto.LoginRequest;
import com.orphanage.oms.auth.dto.LogoutRequest;
import com.orphanage.oms.auth.dto.RefreshTokenRequest;
import com.orphanage.oms.auth.dto.UserResponse;
import com.orphanage.oms.auth.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints for login, Google OAuth, refresh, logout, and current user.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authenticationService.login(request, clientIp(httpRequest), userAgent(httpRequest));
    }

    @PostMapping("/google")
    @Operation(summary = "Login with Google ID token")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest httpRequest) {
        return authenticationService.loginWithGoogle(request, clientIp(httpRequest), userAgent(httpRequest));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a new access token")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return authenticationService.refresh(request, clientIp(httpRequest), userAgent(httpRequest));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token(s) and end the session")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) LogoutRequest request) {
        authenticationService.logout(request != null ? request : new LogoutRequest(null));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    public UserResponse me() {
        return authenticationService.me();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }
}
