package com.papairs.docs.service;

import com.papairs.docs.dto.response.FileResponse;
import com.papairs.docs.exception.FileAlreadyExistsException;
import com.papairs.docs.exception.ResourceNotFoundException;
import com.papairs.docs.exception.StorageException;
import com.papairs.docs.exception.UnauthorizedAccessException;
import com.papairs.docs.model.UserFile;
import com.papairs.docs.repository.FileRepository;
import com.papairs.docs.service.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private final FileStorageService storageService;
    private final FileRepository fileRepository;
    private final UserService userService;

    public FileService(FileStorageService storageService, FileRepository fileRepository, UserService userService) {
        this.storageService = storageService;
        this.fileRepository = fileRepository;
        this.userService = userService;
    }

    @Transactional
    public FileResponse uploadFile(String userId, MultipartFile file) {
        validateUserExists(userId);
        validateFileNotDuplicate(userId, file.getOriginalFilename());

        String fileId = UUID.randomUUID().toString();
        String filePath = buildFilePath(userId, fileId, file.getOriginalFilename());

        try {
            String storageFileId = storageService.upload(
                    file.getBytes(),
                    filePath,
                    file.getContentType()
            );

            UserFile userFile = new UserFile(
                    fileId,
                    userId,
                    file.getOriginalFilename(),
                    storageFileId,
                    filePath,
                    file.getSize(),
                    file.getContentType()
            );

            return FileResponse.of(fileRepository.save(userFile));
        } catch (IOException e) {
            throw new StorageException("Failed to upload file", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FileResponse> getUserFiles(String userId) {
        return fileRepository.findByUserId(userId).stream()
                .map(FileResponse::of)
                .toList();
    }

    @Transactional
    public void deleteFile(String fileId, String userId) {
        UserFile file = findFileById(fileId);
        validateOwnership(file, userId);

        deleteFromStorage(file);
        fileRepository.delete(file);
    }

    @Transactional(readOnly = true)
    public byte[] downloadFile(String fileId, String userId) {
        UserFile file = findFileById(fileId);
        validateOwnership(file, userId);

        return downloadFromStorage(file);
    }

    private void validateUserExists(String userId) {
        if (!userService.userExists(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
    }

    private void validateFileNotDuplicate(String userId, String filename) {
        if (fileRepository.existsByUserIdAndFilename(userId, filename)) {
            throw new FileAlreadyExistsException("File '" + filename + "' already exists");
        }
    }

    private void validateOwnership(UserFile file, String userId) {
        if (!file.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Access denied to this file");
        }
    }

    private UserFile findFileById(String fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
    }

    private String buildFilePath(String userId, String fileId, String filename) {
        return String.format("%s/%s/%s", userId, fileId, filename);
    }

    private void deleteFromStorage(UserFile file) {
        try {
            storageService.delete(file.getB2FilePath(), file.getB2FileId());
        } catch (IOException e) {
            throw new StorageException("Failed to delete file", e);
        }
    }

    private byte[] downloadFromStorage(UserFile file) {
        try {
            return storageService.download(file.getB2FilePath(), file.getB2FileId());
        } catch (IOException e) {
            throw new StorageException("Failed to download file", e);
        }
    }
}
