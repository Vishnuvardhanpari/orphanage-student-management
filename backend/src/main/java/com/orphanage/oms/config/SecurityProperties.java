package com.orphanage.oms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Authentication and JWT configuration properties.
 */
@ConfigurationProperties(prefix = "oms.security")
public record SecurityProperties(
        Jwt jwt,
        Google google,
        Lockout lockout,
        Bootstrap bootstrap
) {

    public record Jwt(
            String secret,
            long accessTokenExpirationMs,
            long refreshTokenExpirationMs,
            String issuer
    ) {
    }

    public record Google(
            String clientId
    ) {
    }

    public record Lockout(
            int maxFailedAttempts,
            long durationMinutes
    ) {
    }

    public record Bootstrap(
            String adminUsername,
            String adminEmail,
            String adminPassword
    ) {
    }
}
