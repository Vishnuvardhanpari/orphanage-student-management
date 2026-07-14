package com.orphanage.oms.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the active {@link StorageService} implementation from {@code oms.storage.type}.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "oms.storage", name = "type", havingValue = "local", matchIfMissing = true)
    public StorageService localFileStorageService(StorageProperties properties) {
        return new LocalFileStorageService(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "oms.storage", name = "type", havingValue = "gcs")
    public StorageService gcsStorageService(StorageProperties properties) {
        return new GcsStorageService(properties);
    }
}
