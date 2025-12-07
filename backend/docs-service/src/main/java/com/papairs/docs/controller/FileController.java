package com.papairs.docs.controller;

import com.papairs.docs.dto.response.FileResponse;
import com.papairs.docs.service.FileService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/docs")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Upload a new file
     * @param file file to upload
     * @param userId user ID from request header
     * @return uploaded file details
     */
    @PostMapping(path = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId
    ) {
        FileResponse response = fileService.uploadFile(userId, file);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Get all files for a user
     * @param userId user ID from request header
     * @return list of user's files
     */
    @GetMapping("/files")
    public ResponseEntity<List<FileResponse>> getUserFiles(
            @RequestHeader("X-User-Id") String userId
    ) {
        List<FileResponse> files = fileService.getUserFiles(userId);
        return ResponseEntity.ok(files);
    }

    /**
     * Delete a file
     * @param fileId file ID to delete
     * @param userId user ID from request header
     * @return no content on success
     */
    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileId,
            @RequestHeader("X-User-Id") String userId
    ) {
        fileService.deleteFile(fileId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Download a file
     * @param fileId file ID to download
     * @param userId user ID from request header
     * @return file content as byte array
     */
    @GetMapping("/files/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String fileId,
            @RequestHeader("X-User-Id") String userId
    ) {
        byte[] content = fileService.downloadFile(fileId, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }
}
