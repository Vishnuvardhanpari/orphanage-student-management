package com.orphanage.oms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orphanage.oms.common.ApiErrorResponse;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class JsonAuthenticationEntryPointTest {

    @Test
    void commenceWritesJsonUnauthorized() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(objectMapper);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ServletOutputStream outputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }

            @Override
            public void write(int b) {
                buffer.write(b);
            }
        };
        when(response.getOutputStream()).thenReturn(outputStream);

        entryPoint.commence(request, response, new InsufficientAuthenticationException("missing"));

        verify(response).setStatus(401);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = objectMapper.readValue(buffer.toString(StandardCharsets.UTF_8), ApiErrorResponse.class);
        assertThat(body.status()).isEqualTo(401);
        assertThat(body.error()).isEqualTo("Unauthorized");
        assertThat(body.message()).contains("Authentication is required");
    }
}
