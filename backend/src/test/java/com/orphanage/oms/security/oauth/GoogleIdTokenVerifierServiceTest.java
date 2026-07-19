package com.orphanage.oms.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orphanage.oms.config.SecurityProperties;
import com.orphanage.oms.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GoogleIdTokenVerifierServiceTest {

    @Test
    void verifyThrowsWhenGoogleClientIdNotConfigured() {
        SecurityProperties props = new SecurityProperties(
                null, new SecurityProperties.Google(""), null, null);
        GoogleIdTokenVerifierService service = new GoogleIdTokenVerifierService(props);

        assertThatThrownBy(() -> service.verify("any-token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(api.getMessage()).contains("not configured");
                });
    }

    @Test
    void verifyThrowsWhenIdTokenBlank() {
        SecurityProperties props = new SecurityProperties(
                null, new SecurityProperties.Google("client-id.apps.googleusercontent.com"), null, null);
        GoogleIdTokenVerifierService service = new GoogleIdTokenVerifierService(props);

        assertThatThrownBy(() -> service.verify("  "))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void verifyThrowsUnauthorizedForInvalidToken() {
        SecurityProperties props = new SecurityProperties(
                null, new SecurityProperties.Google("client-id.apps.googleusercontent.com"), null, null);
        GoogleIdTokenVerifierService service = new GoogleIdTokenVerifierService(props);

        assertThatThrownBy(() -> service.verify("not-a-real-google-id-token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
