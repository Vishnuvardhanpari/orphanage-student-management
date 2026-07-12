package com.orphanage.oms.auth.service;

import com.orphanage.oms.auth.entity.RefreshToken;
import com.orphanage.oms.auth.repository.RefreshTokenRepository;
import com.orphanage.oms.config.SecurityProperties;
import com.orphanage.oms.exception.ApiException;
import com.orphanage.oms.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, rotates, and revokes opaque refresh tokens.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityProperties securityProperties;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            SecurityProperties securityProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.securityProperties = securityProperties;
    }

    /**
     * Creates a new refresh token for the user and returns the raw token value.
     *
     * @param user      authenticated user
     * @param clientIp  optional client IP
     * @param userAgent optional user agent
     * @return raw refresh token (only returned once)
     */
    @Transactional
    public String issue(User user, String clientIp, String userAgent) {
        String rawToken = generateRawToken();
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plusMillis(securityProperties.jwt().refreshTokenExpirationMs()))
                .revoked(false)
                .createdByIp(truncate(clientIp, 45))
                .userAgent(truncate(userAgent, 512))
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Rotates a refresh token. Reuse of a revoked token revokes all tokens for the user.
     *
     * @param rawToken  presented refresh token
     * @param clientIp  optional client IP
     * @param userAgent optional user agent
     * @return pair of user and new raw refresh token
     */
    @Transactional
    public RotatedRefreshToken rotate(String rawToken, String clientIp, String userAgent) {
        String tokenHash = hash(rawToken);
        Optional<RefreshToken> existingOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (existingOpt.isEmpty()) {
            throw unauthorized();
        }

        RefreshToken existing = existingOpt.get();
        if (existing.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}", existing.getUser().getId());
            refreshTokenRepository.revokeAllActiveForUser(existing.getUser().getId());
            throw unauthorized();
        }

        if (existing.isExpired()) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw unauthorized();
        }

        User user = existing.getUser();
        if (!user.isEnabled() || user.isCurrentlyLocked() || !user.isAccountNonLocked()) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden", "Account is disabled or locked.");
        }

        existing.setRevoked(true);
        String newRawToken = generateRawToken();
        RefreshToken replacement = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(newRawToken))
                .expiresAt(Instant.now().plusMillis(securityProperties.jwt().refreshTokenExpirationMs()))
                .revoked(false)
                .createdByIp(truncate(clientIp, 45))
                .userAgent(truncate(userAgent, 512))
                .build();
        refreshTokenRepository.save(replacement);
        existing.setReplacedByTokenId(replacement.getId());
        refreshTokenRepository.save(existing);

        return new RotatedRefreshToken(user, newRawToken);
    }

    /**
     * Revokes a single refresh token if present.
     *
     * @param rawToken refresh token
     */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    /**
     * Revokes all active refresh tokens for a user.
     *
     * @param userId user id
     */
    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllActiveForUser(userId);
    }

    /**
     * Deletes expired or revoked refresh tokens.
     */
    @Transactional
    public void purgeExpired() {
        int removed = refreshTokenRepository.deleteExpiredOrRevoked(Instant.now());
        if (removed > 0) {
            log.debug("Purged {} refresh tokens", removed);
        }
    }

    private ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid or expired refresh token.");
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    /**
     * Result of a successful refresh token rotation.
     */
    public record RotatedRefreshToken(User user, String rawRefreshToken) {
    }
}
