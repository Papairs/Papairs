package com.papairs.docs.service.storage;

import java.io.IOException;

public interface FileStorageService {

    /**
     * Upload a file to storage
     * @param content file content as byte array
     * @param filePath logical path for the file
     * @param mimeType MIME type of the file
     * @return storage-specific file identifier
     */
    String upload(byte[] content, String filePath, String mimeType) throws IOException;

    /**
     * Download a file from storage
     * @param filePath logical path of the file
     * @param storageFileId storage-specific file identifier
     * @return file content as byte array
     */
    byte[] download(String filePath, String storageFileId) throws IOException;

    /**
     * Delete a file from storage
     * @param filePath logical path of the file
     * @param storageFileId storage-specific file identifier
     */
    void delete(String filePath, String storageFileId) throws IOException;
}
