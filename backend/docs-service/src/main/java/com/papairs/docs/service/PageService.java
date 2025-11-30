package com.papairs.docs.service;

import com.papairs.docs.dto.response.PageContentResponse;
import com.papairs.docs.dto.response.PageResponse;
import com.papairs.docs.exception.ResourceNotFoundException;
import com.papairs.docs.model.Page;
import com.papairs.docs.model.enums.MemberRole;
import com.papairs.docs.repository.PageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PageService {
    private final PageRepository pageRepository;
    private final PermissionService permissionService;
    private final ContentService contentService;

    public PageService(PageRepository pageRepository, PermissionService permissionService, ContentService contentService) {
        this.pageRepository = pageRepository;
        this.permissionService = permissionService;
        this.contentService = contentService;
    }

    /**
     * Retrieves a single page by its ID, ensuring the user has access
     * @param pageId The ID of the page to retrieve
     * @param userId The ID of the user requesting the page
     * @return The requested {@link PageResponse} dto
     * @throws ResourceNotFoundException if the page is not found
     */
    public PageContentResponse getPage(String pageId, String userId) {
        permissionService.requirePageAccess(pageId, userId);

        Page page = pageRepository.findById(pageId)
            .orElseThrow(() -> new ResourceNotFoundException("Page not found"));
        return PageContentResponse.of(page);
    }

    /**
     * Retrieves all pages that a user can access, either as an owner or as a member
     * @deprecated Use {@link #getAllAccessiblePagesWithRoles(String)} for optimized queries with user roles
     * @param userId The ID of the user.
     * @return A {@link List} of all accessible {@link Page} entities.
     */
    public List<Page> getAllAccessiblePages(String userId) {
        return pageRepository.findAllAccessibleByUserId(userId);
    }

    /**
     * Retrieves all pages with user roles in a single optimized query
     * @param userId The ID of the user
     * @return A {@link List} of {@link PageResponse} with user roles included
     */
    public List<PageResponse> getAllAccessiblePagesWithRoles(String userId) {
        List<Object[]> results = pageRepository.findAllAccessibleByUserIdWithRole(userId);
        
        return results.stream()
            .map(result -> {
                Page page = (Page) result[0];
                MemberRole role = (MemberRole) result[1];
                return PageResponse.of(page, userId, role);
            })
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all pages shared with the user (where user is not the owner)
     * @param userId The ID of the user
     * @return A {@link List} of {@link PageResponse} with user roles included, excluding owned pages
     */
    public List<PageResponse> getSharedPagesWithRoles(String userId) {
        List<Object[]> results = pageRepository.findAllAccessibleByUserIdWithRole(userId);
        
        return results.stream()
            .filter(result -> {
                Page page = (Page) result[0];
                return !page.getOwnerId().equals(userId); // Exclude pages owned by user
            })
            .map(result -> {
                Page page = (Page) result[0];
                MemberRole role = (MemberRole) result[1];
                return PageResponse.of(page, userId, role);
            })
            .collect(Collectors.toList());
    }

    /**
     * Creates a new page within a specified folder for a given owner
     * @param title The title of the new page
     * @param ownerId The ID of the user who will own the page
     * @param folderId The ID of the parent folder. Can be {@code null} for a page at the root level
     * @return The newly created {@link PageContentResponse} entity
     */
    @Transactional
    public PageContentResponse createPage(String title, String ownerId, String folderId) {
        permissionService.requireFolderAccess(folderId, ownerId);

        Page page = new Page();
        page.setTitle(title);
        page.setFolderId(folderId);
        page.setOwnerId(ownerId);
        Page saved = pageRepository.save(page);
        return PageContentResponse.of(saved);
    }

    /**
     * Updates the content of a specific page
     * @param pageId  The ID of the page to update
     * @param userId  The ID of the user performing the update. Requires edit permission
     * @param content The new content for the page
     * @return The updated {@link PageContentResponse} dto
     */
    @Transactional
    public PageContentResponse updatePage(String pageId, String userId, String content) {
        permissionService.requirePageEdit(pageId, userId);
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found"));
        page.setContent(content);
        Page saved = pageRepository.save(page);

        return PageContentResponse.of(saved);
    }

    /**
     * Renames a page
     * @param pageId The ID of the page to rename
     * @param userId The ID of the user performing the action. Requires edit permission
     * @param newTitle The new title for the page
     * @return The updated {@link PageContentResponse} dto
     * @throws ResourceNotFoundException if the page is not found
     */
    @Transactional
    public PageContentResponse renamePage(String pageId, String userId, String newTitle) {
        permissionService.requirePageEdit(pageId, userId);
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found"));
        page.setTitle(newTitle);
        Page saved = pageRepository.save(page);

        return PageContentResponse.of(saved);
    }

    /**
     * Deletes a page and all its associated memberships
     * @param pageId The ID of the page to delete
     * @param userId The ID of the user performing the action. Requires edit permission
     */
    @Transactional
    public void deletePage(String pageId, String userId) {
        permissionService.requirePageDeletion(pageId, userId);
        contentService.deletePageWithMembers(pageId);
    }

    /**
     * Moves a page to a new folder or to the root level
     * @param pageId The ID of the page to move
     * @param userId The ID of the user performing the move. Requires edit permission on the page
     *               and access to the target folder
     * @param targetFolderId The ID of the destination folder. Can be {@code null} to move to the root
     * @return The updated {@link PageContentResponse} dto
     * @throws ResourceNotFoundException if the page is not found
     */
    @Transactional
    public PageContentResponse movePage(String pageId, String targetFolderId, String userId) {
        permissionService.requirePageEdit(pageId, userId);

        if (targetFolderId != null) {
            permissionService.requireFolderAccess(targetFolderId, userId);
        }

        Page page = pageRepository.findById(pageId)
            .orElseThrow(() -> new ResourceNotFoundException("Page not found"));

        page.setFolderId(targetFolderId);
        Page saved = pageRepository.save(page);
        return PageContentResponse.of(saved);
    }
}
