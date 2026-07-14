package com.orphanage.oms.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * File storage configuration for local filesystem or Google Cloud Storage.
 */
@ConfigurationProperties(prefix = "oms.storage")
public record StorageProperties(
        String type,
        Local local,
        Gcs gcs
) {

    public record Local(String basePath) {
    }

    public record Gcs(String bucket) {
    }
}
