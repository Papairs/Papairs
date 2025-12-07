package com.papairs.docs.service.storage;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

@Service
@ConditionalOnProperty(name = "storage.mode", havingValue = "b2")
public class B2FileStorageService implements FileStorageService {

    private final B2StorageClient b2Client;

    @Value("${b2.bucket-name}")
    private String bucketName;

    private String bucketId;

    public B2FileStorageService(B2StorageClient b2Client) {
        this.b2Client = b2Client;
    }

    @PostConstruct
    public void init() throws B2Exception {
        List<B2Bucket> buckets = b2Client.buckets();
        this.bucketId = buckets.stream()
                .filter(bucket -> bucket.getBucketName().equals(bucketName))
                .findFirst()
                .map(B2Bucket::getBucketId)
                .orElseThrow(() -> new RuntimeException(
                    String.format("B2 bucket '%s' not found", bucketName)));
    }

    @Override
    public String upload(byte[] content, String filePath, String mimeType) throws IOException {
        try {
            B2UploadFileRequest uploadRequest = B2UploadFileRequest
                    .builder(bucketId, filePath, mimeType, B2ByteArrayContentSource.build(content))
                    .build();
            B2FileVersion fileVersion = b2Client.uploadSmallFile(uploadRequest);
            return fileVersion.getFileId();
        } catch (B2Exception e) {
            throw new IOException("Failed to upload to B2: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String filePath, String storageFileId) throws IOException {
        try {
            B2DownloadByIdRequest downloadRequest = B2DownloadByIdRequest.builder(storageFileId).build();
            B2ContentMemoryWriter writer = B2ContentMemoryWriter.build();
            b2Client.downloadById(downloadRequest, writer);
            return writer.getBytes();
        } catch (B2Exception e) {
            throw new IOException("Failed to download from B2: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String filePath, String storageFileId) throws IOException {
        try {
            b2Client.deleteFileVersion(filePath, storageFileId);
        } catch (B2Exception e) {
            throw new IOException("Failed to delete from B2: " + e.getMessage(), e);
        }
    }
}
