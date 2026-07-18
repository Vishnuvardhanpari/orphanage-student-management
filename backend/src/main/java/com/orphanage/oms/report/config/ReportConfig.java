package com.orphanage.oms.report.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables report module configuration properties.
 */
@Configuration
@EnableConfigurationProperties(ReportProperties.class)
public class ReportConfig {
}
