package com.orphanage.oms.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PDF report generation limits and branding.
 */
@ConfigurationProperties(prefix = "oms.reports")
public record ReportProperties(
        String organizationName,
        int maxSelectedStudents,
        int maxFilterResults
) {
}
