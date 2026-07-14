package com.orphanage.oms.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orphanage.oms.exception.ApiException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties(
                "local",
                new StorageProperties.Local(tempDir.toString()),
                new StorageProperties.Gcs(null));
        storageService = new LocalFileStorageService(properties);
    }

    @Test
    void storeAndDeleteFile() throws Exception {
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        String path = storageService.store(
                "student-documents/abc/profile-photo.jpg",
                "image/jpeg",
                new ByteArrayInputStream(content),
                content.length);

        assertThat(path).isEqualTo("student-documents/abc/profile-photo.jpg");
        Path stored = tempDir.resolve("student-documents/abc/profile-photo.jpg");
        assertThat(Files.exists(stored)).isTrue();
        assertThat(Files.readAllBytes(stored)).isEqualTo(content);

        storageService.delete(path);
        assertThat(Files.exists(stored)).isFalse();
    }

    @Test
    void rejectsPathTraversal() {
        byte[] content = "x".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> storageService.store(
                        "../outside.txt",
                        "text/plain",
                        new ByteArrayInputStream(content),
                        content.length))
                .isInstanceOf(ApiException.class);
    }
}
