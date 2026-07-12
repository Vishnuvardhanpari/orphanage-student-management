package com.orphanage.oms.security.jwt;

import com.orphanage.oms.config.SecurityProperties;
import com.orphanage.oms.security.UserPrincipal;
import com.orphanage.oms.user.enums.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Creates and validates JWT access tokens.
 */
@Service
public class JwtService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";

    private final SecurityProperties securityProperties;
    private final SecretKey secretKey;

    public JwtService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        byte[] keyBytes = securityProperties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("oms.security.jwt.secret must be at least 256 bits (32 characters)");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Creates a signed access token for the given principal.
     *
     * @param principal authenticated user
     * @return JWT compact string
     */
    public String createAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(securityProperties.jwt().accessTokenExpirationMs());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(principal.getId().toString())
                .issuer(securityProperties.jwt().issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_USERNAME, principal.getUsername())
                .claim(CLAIM_ROLE, principal.getRole().name())
                .signWith(secretKey)
                .compact();
    }

    /**
     * Parses and validates an access token.
     *
     * @param token JWT compact string
     * @return token claims
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .clockSkewSeconds(30)
                .requireIssuer(securityProperties.jwt().issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns true when the token is syntactically valid and not expired.
     *
     * @param token JWT compact string
     * @return whether the token is valid
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            return false;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String extractUsername(Claims claims) {
        return claims.get(CLAIM_USERNAME, String.class);
    }

    public RoleName extractRole(Claims claims) {
        return RoleName.valueOf(claims.get(CLAIM_ROLE, String.class));
    }

    public long getAccessTokenExpirationMs() {
        return securityProperties.jwt().accessTokenExpirationMs();
    }
}
