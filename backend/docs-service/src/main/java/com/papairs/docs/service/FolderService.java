package com.papairs.docs.service;

import com.papairs.docs.dto.response.FolderResponse;
import com.papairs.docs.dto.response.FolderTreeResponse;
import com.papairs.docs.dto.response.NavigationItemResponse;
import com.papairs.docs.exception.InvalidRequestException;
import com.papairs.docs.exception.ResourceConflictException;
import com.papairs.docs.exception.ResourceNotFoundException;
import com.papairs.docs.model.Folder;
import com.papairs.docs.repository.FolderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.papairs.docs.model.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FolderService {
    private final FolderRepository folderRepository;
    private final PermissionService permissionService;
    private final ContentService contentService;

    public FolderService(FolderRepository folderRepository, PermissionService permissionService, ContentService contentService) {
        this.folderRepository = folderRepository;
        this.permissionService = permissionService;
        this.contentService = contentService;
    }

    /**
     * Creates a new folder
     * The user must have access to the parent folder, if one is specified
     * @param name The name for the new folder
     * @param ownerId The ID of the user who will own the folder
     * @param parentFolderId The ID of the parent folder. Can be {@code null} to create a root folder.
     * @return The newly created {@link FolderResponse}
     */
    @Transactional
    public FolderResponse createFolder(String name, String ownerId, String parentFolderId) {
        if (parentFolderId != null) {
            permissionService.requireFolderAccess(parentFolderId, ownerId);
        }

        Folder folder = new Folder();
        folder.setName(name);
        folder.setOwnerId(ownerId);
        folder.setParentFolderId(parentFolderId);

        Folder saved = folderRepository.save(folder);
        return FolderResponse.of(saved);
    }

    /**
     * Retrieves a single folder by its ID by ensuring the user has access
     * @param folderId The ID of the folder to retrieve
     * @param userId The ID of the user making the request
     * @return The requested {@link FolderResponse}
     * @throws ResourceNotFoundException if the folder is not found
     */
    public FolderResponse getFolder(String folderId, String userId) {
        permissionService.requireFolderAccess(folderId, userId);

        Folder folder = folderRepository.findById(folderId)
            .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        
        return FolderResponse.of(folder);
    }

    /**
     * Renames an existing folder
     * @param folderId The ID of the folder to rename
     * @param userId The ID of the user performing the action.
     * @param newName The new name for the folder
     * @return The updated {@link FolderResponse}
     * @throws ResourceNotFoundException if the folder is not found
     */
    @Transactional
    public FolderResponse renameFolder(String folderId, String userId, String newName) {
        permissionService.requireFolderAccess(folderId, userId);
        
        Folder folder = folderRepository.findById(folderId)
            .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        folder.setName(newName);

        Folder saved = folderRepository.save(folder);
        return FolderResponse.of(saved);
    }

    /**
     * Deletes a folder
     * Fails if the folder is not empty, unless recursive delete is specified
     * @param folderId The ID of the folder to delete
     * @param userId The ID of the user performing the action
     * @param recursive If {@code true}, all subfolders and pages will be deleted
     *                  If {@code false}, the operation will fail if the folder is not empty
     * @throws ResourceNotFoundException   if the folder does not exist
     * @throws ResourceConflictException if {@code recursive} is false and the folder contains content
     */
    @Transactional
    public void deleteFolder(String folderId, String userId, boolean recursive) {
        permissionService.requireFolderAccess(folderId, userId);

        if (!folderRepository.existsById(folderId)) {
            throw new ResourceNotFoundException("Folder not found");
        }

        if (recursive) {
            contentService.deleteFolderWithContents(folderId);
        } else {
            if (contentService.folderHasContent(folderId)) {
                throw new ResourceConflictException("Folder is not empty");
            }
            folderRepository.deleteById(folderId);
        }
    }

    /**
     * Retrieves all root-level folders for a given user
     * @param userId The ID of the user
     * @return A {@link List} of root {@link FolderResponse}
     */
    public List<FolderResponse> getRootFolders(String userId) {
        List<Folder> folders = folderRepository.findByOwnerIdAndParentFolderIdIsNull(userId);
        return folders.stream()
            .map(FolderResponse::of)
            .collect(Collectors.toList());
    }

    /**
     * Get all folders for a user
     * @param userId The ID of the user
     * @return A {@link List} of {@link FolderResponse}
     */
    public List<FolderResponse> getAllUserFolders(String userId) {
        List<Folder> folders = folderRepository.findByOwnerId(userId);
        return folders.stream()
            .map(FolderResponse::of)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all direct child folders of a given parent folder
     * @param folderId The ID of the parent folder
     * @param userId The ID of the user requesting the children
     * @return A {@link List} of child {@link FolderResponse}
     */
    public List<FolderResponse> getChildFolders(String folderId, String userId) {
        permissionService.requireFolderAccess(folderId, userId);
        List<Folder> children = folderRepository.findByParentFolderId(folderId);
        return children.stream()
            .map(FolderResponse::of)
            .collect(Collectors.toList());
    }

    /**
     * Get folder tree starting from a folder
     * Uses a single recursive query to fetch all descendants at once,
     * then builds the tree in memory without additional database calls.
     * @param folderId folder ID
     * @param userId user ID
     * @return FolderTreeResponse structure
     * @throws ResourceNotFoundException if the folder does not exist
     */
    public FolderTreeResponse getFolderTree(String folderId, String userId) {
        permissionService.requireFolderAccess(folderId, userId);

        // Get folder and descendants
        List<String> descendantIds = folderRepository.findAllDescendantIds(folderId);
        List<String> allFolderIds = new ArrayList<>(descendantIds);
        allFolderIds.add(folderId);

        // Build tree structure
        List<Folder> allFolders = folderRepository.findAllById(allFolderIds);
        if (CollectionUtils.isEmpty(allFolders)) {
            throw new ResourceNotFoundException("Folder not found, or its descendants are missing");
        }

        // Get page counts from ContentService
        Folder rootFolder = allFolders.stream()
            .filter(f -> f.getFolderId().equals(folderId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        Map<String, List<Folder>> childrenMap = allFolders.stream()
            .filter(f -> f.getParentFolderId() != null)
            .collect(Collectors.groupingBy(Folder::getParentFolderId));

        // Get page counts for all relevant folders in another single batch query.
        Map<String, Long> pageCountMap = contentService.getPageCountsForFolders(allFolderIds);
        
        // Get pages for all folders
        Map<String, List<Page>> pagesByFolderId = contentService.getPagesForFolders(allFolderIds);

        // Build and return the tree structure recursively in memory.
        return buildFolderTreeResponse(rootFolder, childrenMap, pageCountMap, pagesByFolderId);
    }

    /**
     * Constructs and retrieves the entire hierarchical tree of all folders owned by a user.
     * @param userId The ID of the user.
     * @return A {@link List} of {@link FolderTreeResponse} objects, one for each root folder.
     */
     public List<FolderTreeResponse> getUserFolderTrees(String userId) {
        List<Folder> allFolders = folderRepository.findByOwnerId(userId);
        if (CollectionUtils.isEmpty(allFolders)) {
            return Collections.emptyList();
        }
    
        List<Folder> rootFolders = allFolders.stream()
            .filter(f -> f.getParentFolderId() == null)
            .collect(Collectors.toList());
    
        Map<String, List<Folder>> childrenMap = allFolders.stream()
            .filter(f -> f.getParentFolderId() != null)
            .collect(Collectors.groupingBy(Folder::getParentFolderId));
    
        List<String> folderIds = allFolders.stream()
            .map(Folder::getFolderId)
            .collect(Collectors.toList());
    
        Map<String, Long> pageCountMap = contentService.getPageCountsForFolders(folderIds);
    
        Map<String, List<Page>> pagesByFolderId =
            contentService.getPagesForFolders(folderIds);

        // Use buildFolderTreeResponse instead of buildFolderTree
        return rootFolders.stream()
            .map(root -> buildFolderTreeResponse(root, childrenMap, pageCountMap, pagesByFolderId))
            .collect(Collectors.toList());
    }
    

    /**
     * Move a folder to a new parent folder
     * @param folderId folder ID
     * @param userId user ID
     * @param newParentFolderId new parent folder ID
     * @return Updated {@link FolderResponse}
     * @throws InvalidRequestException if attempting to move a folder into itself or its descendants
     */
    @Transactional
    public FolderResponse moveFolder(String folderId, String userId, String newParentFolderId) {
        permissionService.requireFolderAccess(folderId, userId);
        
        Folder folderToMove = folderRepository.findById(folderId)
            .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        if (newParentFolderId != null) {
            permissionService.requireFolderAccess(newParentFolderId, userId);

            if (folderId.equals(newParentFolderId)) {
                throw new InvalidRequestException("Cannot move a folder into itself");
            }

            List<String> descendantIds = folderRepository.findAllDescendantIds(folderId);
            if (descendantIds.contains(newParentFolderId)) {
                throw new InvalidRequestException("Cannot move a folder into one of its own descendants");
            }
        }

        folderToMove.setParentFolderId(newParentFolderId);
        Folder saved = folderRepository.save(folderToMove);
        return FolderResponse.of(saved);
    }

    /**
     * Retrieves the ancestor path from the root down to the specified folder
     * @param folderId The ID of the target folder
     * @param userId The ID of the user making the request
     * @return A {@link List} of {@link FolderResponse} representing the path, ordered from root to target
     */
    public List<FolderResponse> getFolderPath(String folderId, String userId) {
        permissionService.requireFolderAccess(folderId, userId);
        List<Folder> path = folderRepository.findAncestorPath(folderId);
        Collections.reverse(path); // Reverse to get root -> target order
        return path.stream()
            .map(FolderResponse::of)
            .collect(Collectors.toList());
    }

    /**
     * Build folder tree recursively using pre-fetched data
     * No database queries - uses in-memory maps
     * @param folder folder entity
     * @param childrenMap map of parent folder ID to list of child folders
     * @param pageCountMap map of folder ID to page count
     * @param pagesByFolderId map of folder ID to list of pages
     * @return {@link FolderTreeResponse}
     */
    private FolderTreeResponse buildFolderTreeResponse(
        Folder folder,
        Map<String, List<Folder>> childrenMap,
        Map<String, Long> pageCountMap,
        Map<String, List<Page>> pagesByFolderId
    ) {
        // Add documents by mapping Pages to NavigationItemResponse
        List<Page> pages = pagesByFolderId.getOrDefault(folder.getFolderId(), Collections.emptyList());
        List<NavigationItemResponse> documents = pages.stream()
                .map(NavigationItemResponse::from)
                .collect(Collectors.toList());

        // Build children recursively
        List<Folder> children = childrenMap.getOrDefault(folder.getFolderId(), new ArrayList<>());
        List<FolderTreeResponse> childResponses = new ArrayList<>();

        for (Folder child : children) {
            childResponses.add(buildFolderTreeResponse(child, childrenMap, pageCountMap, pagesByFolderId));
        }

        return new FolderTreeResponse(
                folder.getFolderId(),
                folder.getName(),
                folder.getCreatedAt(),
                pageCountMap.getOrDefault(folder.getFolderId(), 0L),
                childResponses.size(),
                childResponses,
                documents
        );
    }
}
