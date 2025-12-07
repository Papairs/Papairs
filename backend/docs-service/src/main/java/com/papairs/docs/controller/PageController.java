package com.papairs.docs.controller;

import com.papairs.docs.dto.request.CreatePageRequest;
import com.papairs.docs.dto.request.MovePageRequest;
import com.papairs.docs.dto.request.RenamePageRequest;
import com.papairs.docs.dto.request.UpdatePageRequest;
import com.papairs.docs.dto.response.PageContentResponse;
import com.papairs.docs.dto.response.PageResponse;
import com.papairs.docs.security.HtmlSanitizer;
import com.papairs.docs.service.PageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docs")
public class PageController {
    private final PageService pageService;
    private final HtmlSanitizer htmlSanitizer;

    public PageController(PageService pageService, HtmlSanitizer htmlSanitizer) {
        this.pageService = pageService;
        this.htmlSanitizer = htmlSanitizer;
    }

    /**
     * Get list of pages which the user has access to
     * @param userId user ID from request header
     * @return ResponseEntity with list of pages with user roles
     */
    @GetMapping("/pages")
    public ResponseEntity<List<PageResponse>> getUserPages(@RequestHeader("X-User-Id") String userId) {
        List<PageResponse> pages = pageService.getAllAccessiblePagesWithRoles(userId);
        return ResponseEntity.ok(pages);
    }

    /**
     * Get list of pages shared with the user (where user is not the owner)
     * @param userId user ID from request header
     * @return ResponseEntity with list of shared pages
     */
    @GetMapping("/pages/shared")
    public ResponseEntity<List<PageResponse>> getSharedPages(@RequestHeader("X-User-Id") String userId) {
        List<PageResponse> pages = pageService.getSharedPagesWithRoles(userId);
        return ResponseEntity.ok(pages);
    }

    /**
     * Create a new page
     * @param createPageRequest create page request
     * @param userId user ID from request header
     * @return ResponseEntity with created page
     */
    @PostMapping("/pages")
    public ResponseEntity<PageContentResponse> createPage(
            @Valid @RequestBody CreatePageRequest createPageRequest,
            @RequestHeader("X-User-Id") String userId
    ) {
        PageContentResponse page = pageService.createPage(
            createPageRequest.getTitle(),
            userId,
            createPageRequest.getFolderId()
        );
        return ResponseEntity.status(201).body(page);
    }

    /**
     * Get a page by ID
     * @param pageId page ID
     * @param userId user ID from request header
     * @return ResponseEntity with the page
     */
    @GetMapping("/pages/{pageId}")
    public ResponseEntity<PageContentResponse> getPage(
            @PathVariable String pageId,
            @RequestHeader("X-User-Id") String userId
    ) {
        PageContentResponse page = pageService.getPage(pageId, userId);
        return ResponseEntity.ok(page);
    }

    /**
     * Update a page's content
     * @deprecated Use the Websocket implementation for real-time updates {@link com.papairs.docs.ws.DocWebSocketHandler}
     * @param pageId page ID
     * @param updatePageRequest update page request
     * @param userId user ID from request header
     * @return ResponseEntity with updated page
     */
    @PutMapping("/pages/{pageId}")
    public ResponseEntity<PageContentResponse> updatePage(
            @PathVariable String pageId,
            @RequestBody UpdatePageRequest updatePageRequest,
            @RequestHeader("X-User-Id") String userId
    ) {
        String sanitizedContent = htmlSanitizer.sanitize(updatePageRequest.getContent());
        PageContentResponse updated = pageService.updatePage(pageId, userId, sanitizedContent);
        return ResponseEntity.ok(updated);
    }

    /**
     * Rename a page
     * @param pageId page ID
     * @param renamePageRequest rename page request
     * @param userId user ID from request header
     * @return ResponseEntity with renamed page
     */
    @PatchMapping("/pages/{pageId}")
    public ResponseEntity<PageContentResponse> renamePage(
            @PathVariable String pageId,
            @Valid @RequestBody RenamePageRequest renamePageRequest,
            @RequestHeader("X-User-Id") String userId
    ) {
        PageContentResponse renamed = pageService.renamePage(
            pageId,
            userId,
            renamePageRequest.getNewTitle()
        );
        return ResponseEntity.ok(renamed);
    }

    /**
     * Move a page to a new folder
     * @param pageId page ID
     * @param movePageRequest move page request
     * @param userId user ID from request header
     * @return ResponseEntity with moved page
     */
    @PatchMapping("/pages/{pageId}/move")
    public ResponseEntity<PageContentResponse> movePage(
            @PathVariable String pageId,
            @Valid @RequestBody MovePageRequest movePageRequest,
            @RequestHeader("X-User-Id") String userId
    ) {
        PageContentResponse moved = pageService.movePage(
            pageId,
            movePageRequest.getFolderId(),
            userId
        );
        return ResponseEntity.ok(moved);
    }

    /**
     * Delete a page by ID
     * @param pageId page ID
     * @param userId user ID from request header
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/pages/{pageId}")
    public ResponseEntity<Void> deletePage(
            @PathVariable String pageId,
            @RequestHeader("X-User-Id") String userId
    ) {
        pageService.deletePage(pageId, userId);
        return ResponseEntity.noContent().build();
    }
}
