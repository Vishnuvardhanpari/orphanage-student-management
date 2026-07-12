package com.orphanage.oms.security.oauth;

/**
 * Verified Google identity extracted from an ID token.
 */
public record GoogleIdentity(
        String subject,
        String email,
        boolean emailVerified
) {
}
