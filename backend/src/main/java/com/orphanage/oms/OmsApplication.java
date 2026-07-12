package com.orphanage.oms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Orphanage Management System API.
 */
@SpringBootApplication(exclude = OAuth2ClientAutoConfiguration.class)
@EnableScheduling
public class OmsApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(OmsApplication.class, args);
    }
}
