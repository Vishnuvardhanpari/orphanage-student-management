package com.orphanage.oms.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.orphanage.oms.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Stores files in a Google Cloud Storage bucket via the JSON API (HTTP).
 *
 * <p>Uses Application Default Credentials ({@code GOOGLE_APPLICATION_CREDENTIALS} or the runtime
 * service account on Cloud Run).
 */
public class GcsStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(GcsStorageService.class);
    private static final String STORAGE_SCOPE = "https://www.googleapis.com/auth/devstorage.read_write";

    private final String bucket;
    private final HttpClient httpClient;
    private final GoogleCredentials credentials;

    public GcsStorageService(StorageProperties properties) {
        if (properties.gcs() == null || properties.gcs().bucket() == null
                || properties.gcs().bucket().isBlank()) {
            throw new IllegalStateException(
                    "oms.storage.gcs.bucket (GCS_BUCKET_NAME) must be configured for GCS storage.");
        }
        this.bucket = properties.gcs().bucket().trim();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        try {
            this.credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(List.of(STORAGE_SCOPE));
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to load Google Application Default Credentials for GCS storage.", ex);
        }
        log.info("GCS HTTP storage initialized for bucket={}", this.bucket);
    }

    GcsStorageService(StorageProperties properties, HttpClient httpClient, GoogleCredentials credentials) {
        this.bucket = properties.gcs().bucket().trim();
        this.httpClient = httpClient;
        this.credentials = credentials;
    }

    @Override
    public String store(String relativePath, String contentType, InputStream content, long size) {
        String objectName = normalizeRelative(relativePath);
        try {
            byte[] bytes = content.readAllBytes();
            String encodedName = URLEncoder.encode(objectName, StandardCharsets.UTF_8).replace("+", "%20");
            URI uri = URI.create(
                    "https://storage.googleapis.com/upload/storage/v1/b/"
                            + URLEncoder.encode(bucket, StandardCharsets.UTF_8)
                            + "/o?uploadType=media&name="
                            + encodedName);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMinutes(2))
                    .header("Authorization", "Bearer " + accessToken())
                    .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("GCS upload failed status={} body={}", response.statusCode(), response.body());
                throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Storage Error",
                        "Failed to store file in Google Cloud Storage.");
            }
            return objectName;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Storage Error",
                    "Failed to store file in Google Cloud Storage.");
        }
    }

    @Override
    public void delete(String relativePath) {
        String objectName = normalizeRelative(relativePath);
        try {
            String encodedName = URLEncoder.encode(objectName, StandardCharsets.UTF_8).replace("+", "%20");
            URI uri = URI.create(
                    "https://storage.googleapis.com/storage/v1/b/"
                            + URLEncoder.encode(bucket, StandardCharsets.UTF_8)
                            + "/o/"
                            + encodedName);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + accessToken())
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Failed to delete GCS object path={} status={}", objectName, response.statusCode());
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Failed to delete GCS object path={}", objectName);
        }
    }

    @Override
    public InputStream load(String relativePath) {
        String objectName = normalizeRelative(relativePath);
        try {
            String encodedName = URLEncoder.encode(objectName, StandardCharsets.UTF_8).replace("+", "%20");
            URI uri = URI.create(
                    "https://storage.googleapis.com/storage/v1/b/"
                            + URLEncoder.encode(bucket, StandardCharsets.UTF_8)
                            + "/o/"
                            + encodedName
                            + "?alt=media");

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMinutes(2))
                    .header("Authorization", "Bearer " + accessToken())
                    .GET()
                    .build();

            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 404) {
                try (InputStream ignored = response.body()) {
                    // discard
                }
                throw new ApiException(HttpStatus.NOT_FOUND, "Not Found", "Stored file was not found.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                try (InputStream errorBody = response.body()) {
                    log.error("GCS download failed status={}", response.statusCode());
                }
                throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Storage Error",
                        "Failed to read file from Google Cloud Storage.");
            }
            return response.body();
        } catch (ApiException ex) {
            throw ex;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Storage Error",
                    "Failed to read file from Google Cloud Storage.");
        }
    }

    private String accessToken() throws IOException {
        credentials.refreshIfExpired();
        if (credentials.getAccessToken() == null || credentials.getAccessToken().getTokenValue() == null) {
            throw new IOException("Google credentials did not provide an access token.");
        }
        return credentials.getAccessToken().getTokenValue();
    }

    private static String normalizeRelative(String relativePath) {
        return relativePath.replace('\\', '/').replaceAll("^/+", "");
    }
}
