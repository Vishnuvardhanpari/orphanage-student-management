package com.orphanage.oms.security.oauth;

/**
 * Verifies Google ID tokens issued by Google Identity Services.
 */
public interface GoogleTokenVerifier {

    /**
     * Verifies the ID token and returns the Google identity.
     *
     * @param idToken raw Google ID token
     * @return verified identity
     */
    GoogleIdentity verify(String idToken);
}
