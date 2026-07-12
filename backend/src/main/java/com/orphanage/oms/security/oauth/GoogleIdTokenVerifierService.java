package com.orphanage.oms.security.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.orphanage.oms.config.SecurityProperties;
import com.orphanage.oms.exception.ApiException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Verifies Google ID tokens using Google's public keys.
 */
@Service
public class GoogleIdTokenVerifierService implements GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;
    private final String clientId;

    public GoogleIdTokenVerifierService(SecurityProperties securityProperties) {
        this.clientId = securityProperties.google().clientId();
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        if (!StringUtils.hasText(clientId)) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Configuration Error",
                    "Google OAuth is not configured.");
        }
        if (!StringUtils.hasText(idToken)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validation Error", "idToken is required.");
        }

        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid Google ID token.");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            Boolean emailVerified = payload.getEmailVerified();

            if (!StringUtils.hasText(email) || !Boolean.TRUE.equals(emailVerified)) {
                throw new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        "Google account email is missing or not verified.");
            }

            return new GoogleIdentity(payload.getSubject(), email.toLowerCase(), true);
        } catch (ApiException ex) {
            throw ex;
        } catch (GeneralSecurityException | IOException | IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid Google ID token.");
        }
    }
}
