package com.orphanage.oms.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void resolveReturnsNullForNullRequest() {
        assertThat(resolver.resolve(null)).isNull();
    }

    @Test
    void resolveUsesFirstXForwardedForHop() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.1");
    }

    @Test
    void resolveFallsBackToRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void resolveTruncatesLongIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        String longValue = "a".repeat(50);
        when(request.getHeader("X-Forwarded-For")).thenReturn(longValue);

        assertThat(resolver.resolve(request)).hasSize(45);
    }

    @Test
    void resolveCurrentUsesRequestContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(resolver.resolveCurrent()).isEqualTo("10.0.0.5");
    }

    @Test
    void resolveCurrentReturnsNullWithoutContext() {
        assertThat(resolver.resolveCurrent()).isNull();
    }
}
