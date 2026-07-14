package com.orphanage.oms.storage;

import java.io.InputStream;

/**
 * Abstraction over object/file storage used for student photos and documents.
 */
public interface StorageService {

    /**
     * Stores content at the given relative path.
     *
     * @param relativePath storage-relative path (forward slashes)
     * @param contentType  MIME type
     * @param content      file content stream
     * @param size         content length in bytes
     * @return the stored relative path
     */
    String store(String relativePath, String contentType, InputStream content, long size);

    /**
     * Deletes an object if it exists. Missing objects are ignored.
     *
     * @param relativePath storage-relative path
     */
    void delete(String relativePath);

    /**
     * Opens a stream to a previously stored object.
     *
     * <p>Callers must close the returned stream. Missing objects yield a not-found
     * {@link com.orphanage.oms.exception.ApiException}.
     *
     * @param relativePath storage-relative path
     * @return stream of object bytes
     */
    InputStream load(String relativePath);
}
