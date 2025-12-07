package com.papairs.docs.controller;

import com.papairs.docs.dto.request.CreateFolderRequest;
import com.papairs.docs.dto.request.MoveFolderRequest;
import com.papairs.docs.dto.request.RenameFolderRequest;
import com.papairs.docs.dto.response.FolderResponse;
import com.papairs.docs.dto.response.FolderTreeResponse;
import com.papairs.docs.service.FolderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docs")
public class FolderController {
    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    /**
     * Get a folder by ID
     * @param folderId folder ID
     * @param userId user ID from request header
     * @return ResponseEntity with the folder
     */
    @GetMapping("/folders/{folderId}")
    public ResponseEntity<FolderResponse> getFolder(
            @PathVariable String folderId,
            @RequestHeader("X-User-Id") String userId
    ) {
        FolderResponse folder = folderService.getFolder(folderId, userId);
        return ResponseEntity.ok(folder);
    }

    /**
     * Create a new folder
     * @param createFolderRequest create folder request
     * @param userId user ID from request header
     * @return ResponseEntity with created folder
     */
    @PostMapping("/folders")
    public ResponseEntity<FolderResponse> createFolder(
            @Valid @RequestBody CreateFolderRequest createFolderRequest,
            @RequestHeader("X-User-Id") String userId
    ) {
        FolderResponse folder = folderService.createFolder(
            createFolderRequest.getName(),
            userId,
            createFolderRequest.getParentFolderId()
        );
        return ResponseEntity.status(201).body(folder);
    }

    /**
     * Rename a folder
     * @param folderId folder ID
     * @param renameFolderRequest rename folder request
     * @param userId user ID from request header
     * @return ResponseEntity with renamed folder
     */
    @PatchMapping("/folders/{folderId}")
    public ResponseEntity<FolderResponse> renameFolder(
            @PathVariable String folderId,
            @Valid @RequestBody RenameFolderRequest renameFolderRequest,
            @RequestHeader("X-User-Id") String userId
    ) {
        FolderResponse renamed = folderService.renameFolder(
            folderId,
            userId,
            renameFolderRequest.getNewName()
        );
        return ResponseEntity.ok(renamed);
    }

    /**
     * Delete a folder
     * Delete recursively if specified, which will delete all subfolders and pages within.
     * @param folderId folder ID
     * @param recursive whether to delete recursively. Default is false.
     * @param userId user ID from request header
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable String folderId,
            @RequestParam(required = false, defaultValue = "false") boolean recursive,
            @RequestHeader("X-User-Id") String userId
    ) {
        folderService.deleteFolder(
            folderId,
            userId,
            recursive
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all folders for the user
     * @param userId user ID from request header
     * @return ResponseEntity with list of folders
     */
    @GetMapping("/folders")
    public ResponseEntity<List<FolderResponse>> getAllUserFolders(@RequestHeader("X-User-Id") String userId) {
        List<FolderResponse> folders = folderService.getAllUserFolders(userId);
        return ResponseEntity.ok(folders);
    }

    /**
     * Get root folders for the user
     * @param userId user ID from request header
     * @return ResponseEntity with list of root folders
     */
    @GetMapping("/folders/roots")
    public ResponseEntity<List<FolderResponse>> getRootFolders(@RequestHeader("X-User-Id") String userId) {
        List<FolderResponse> folders = folderService.getRootFolders(userId);
        return ResponseEntity.ok(folders);
    }

    /**
     * Get child folders of a folder
     * @param folderId folder ID
     * @param userId user ID from request header
     * @return ResponseEntity with list of child folders
     */
    @GetMapping("/folders/{folderId}/children")
    public ResponseEntity<List<FolderResponse>> getChildFolders(
            @PathVariable String folderId,
            @RequestHeader("X-User-Id") String userId
    ) {
        List<FolderResponse> children = folderService.getChildFolders(folderId, userId);
        return ResponseEntity.ok(children);
    }

    /**
     * Get folder tree starting from a folder
     * @param folderId folder ID
     * @param userId user ID from request header
     * @return ResponseEntity with folder tree
     */
    @GetMapping("/folders/{folderId}/tree")
    public ResponseEntity<FolderTreeResponse> getFolderTree(
            @PathVariable String folderId,
            @RequestHeader("X-User-Id") String userId
    ) {
        FolderTreeResponse tree = folderService.getFolderTree(folderId, userId);
        return ResponseEntity.ok(tree);
    }

    /**
     * Get all folder trees for the user
     * @param userId user ID from request header
     * @return ResponseEntity with list of folder trees
     */
    @GetMapping("/folders/trees")
    public ResponseEntity<List<FolderTreeResponse>> getUserFolderTrees(@RequestHeader("X-User-Id") String userId) {
        List<FolderTreeResponse> trees = folderService.getUserFolderTrees(userId);
        return ResponseEntity.ok(trees);
    }

    /**
     * Get folder path from root to the specified folder
     * @param folderId folder ID
     * @param userId user ID from request header
     * @return ResponseEntity with list of folders in the path
     */
    @GetMapping("/folders/{folderId}/path")
    public ResponseEntity<List<FolderResponse>> getFolderPath(
            @PathVariable String folderId,
            @RequestHeader("X-User-Id") String userId
    ) {
        List<FolderResponse> path = folderService.getFolderPath(
            folderId,
            userId
        );
        return ResponseEntity.ok(path);
    }

    /**
     * Move a folder to a new parent folder
     * @param folderId folder ID
     * @param moveFolderRequest move folder request
     * @return ResponseEntity with moved folder
     */
    @PatchMapping("/folders/{folderId}/move")
    public ResponseEntity<FolderResponse> moveFolder(
            @PathVariable String folderId,
            @Valid @RequestBody MoveFolderRequest moveFolderRequest,
            @RequestHeader("X-User-Id") String userId
    ) {
        FolderResponse moved = folderService.moveFolder(
            folderId,
            userId,
            moveFolderRequest.getParentFolderId()
        );
        return ResponseEntity.ok(moved);
    }
}
