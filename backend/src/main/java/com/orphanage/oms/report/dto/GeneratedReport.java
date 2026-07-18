package com.orphanage.oms.report.dto;

/**
 * Generated PDF payload returned to the controller for download streaming.
 */
public record GeneratedReport(
        byte[] content,
        String fileName
) {
}
