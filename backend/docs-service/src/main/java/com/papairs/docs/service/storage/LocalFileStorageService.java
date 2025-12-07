package com.papairs.docs.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@ConditionalOnProperty(name = "storage.mode", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${storage.local-path:./uploads}")
    private String localStoragePath;

    @PostConstruct
    public void init() throws IOException {
        Path uploadPath = Paths.get(localStoragePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    @Override
    public String upload(byte[] content, String filePath, String mimeType) throws IOException {
        Path fullPath = Paths.get(localStoragePath, filePath);
        Files.createDirectories(fullPath.getParent());
        Files.write(fullPath, content);
        return "local_" + extractFileId(filePath);
    }

    @Override
    public byte[] download(String filePath, String storageFileId) throws IOException {
        Path fullPath = Paths.get(localStoragePath, filePath);
        if (!Files.exists(fullPath)) {
            throw new IOException("File not found in local storage: " + filePath);
        }
        return Files.readAllBytes(fullPath);
    }

    @Override
    public void delete(String filePath, String storageFileId) throws IOException {
        Path fullPath = Paths.get(localStoragePath, filePath);
        if (Files.exists(fullPath)) {
            Files.delete(fullPath);
            cleanupEmptyDirectories(fullPath);
        }
    }

    private String extractFileId(String filePath) {
        String[] parts = filePath.split("/");
        return parts.length >= 2 ? parts[1] : filePath;
    }

    private void cleanupEmptyDirectories(Path file) throws IOException {
        Path parentDir = file.getParent();
        if (parentDir != null && Files.list(parentDir).findAny().isEmpty()) {
            Files.delete(parentDir);
            Path grandParentDir = parentDir.getParent();
            if (grandParentDir != null && Files.list(grandParentDir).findAny().isEmpty()) {
                Files.delete(grandParentDir);
            }
        }
    }
}
