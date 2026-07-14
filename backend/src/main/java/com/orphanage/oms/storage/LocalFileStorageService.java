package com.orphanage.oms.storage;

import com.orphanage.oms.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Stores files under a configurable local base directory.
 */
public class LocalFileStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path basePath;

    public LocalFileStorageService(StorageProperties properties) {
        if (properties.local() == null || properties.local().basePath() == null
                || properties.local().basePath().isBlank()) {
            throw new IllegalStateException("oms.storage.local.base-path must be configured for local storage.");
        }
        this.basePath = Path.of(properties.local().basePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create local storage directory: " + this.basePath, ex);
        }
        log.info("Local file storage initialized at {}", this.basePath);
    }

    @Override
    public String store(String relativePath, String contentType, InputStream content, long size) {
        Path target = resolveSafe(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return normalizeRelative(relativePath);
        } catch (IOException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Storage Error",
                    "Failed to store file.");
        }
    }

    @Override
    public void delete(String relativePath) {
        Path target = resolveSafe(relativePath);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("Failed to delete local file path={}", relativePath);
        }
    }

    @Override
    public InputStream load(String relativePath) {
        Path target = resolveSafe(relativePath);
        if (!Files.isRegularFile(target)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Not Found", "Stored file was not found.");
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Storage Error",
                    "Failed to read stored file.");
        }
    }

    private Path resolveSafe(String relativePath) {
        String normalized = normalizeRelative(relativePath);
        Path resolved = basePath.resolve(normalized).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validation Error", "Invalid storage path.");
        }
        return resolved;
    }

    private static String normalizeRelative(String relativePath) {
        return relativePath.replace('\\', '/').replaceAll("^/+", "");
    }
}
