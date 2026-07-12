package com.orphanage.oms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration for the OMS API.
 */
@Configuration
public class OpenApiConfig {

    /** API path prefix for controllers (e.g. {@code @RequestMapping("/api/v1/...")}). */
    public static final String API_BASE_PATH = "/api/v1";

    private static final String BEARER_SCHEME = "bearerAuth";

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * Configures the OpenAPI document metadata and server information.
     * Server URL is the application root; controller paths include {@link #API_BASE_PATH}.
     *
     * @return the OpenAPI bean
     */
    @Bean
    public OpenAPI omsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Orphanage Management System API")
                        .description("REST API for managing orphanage student records. "
                                + "All business endpoints are under " + API_BASE_PATH + ". "
                                + "Authenticate via /api/v1/auth/login or /api/v1/auth/google, "
                                + "then send Authorization: Bearer <accessToken>.")
                        .version("v1")
                        .contact(new Contact()
                                .name("OMS Team"))
                        .license(new License()
                                .name("Proprietary")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token from /api/v1/auth/login, /auth/google, or /auth/refresh")));
    }
}
