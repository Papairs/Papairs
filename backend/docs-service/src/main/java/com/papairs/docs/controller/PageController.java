package com.papairs.docs.controller;

import com.papairs.docs.dto.request.CreatePageRequest;
import com.papairs.docs.dto.request.MovePageRequest;
import com.papairs.docs.dto.request.RenamePageRequest;
import com.papairs.docs.dto.request.UpdatePageRequest;
import com.papairs.docs.dto.response.PageContentResponse;
import com.papairs.docs.dto.response.PageResponse;
import com.papairs.docs.security.HtmlSanitizer;
import com.papairs.docs.service.PageService;
import com.papairs.docs.util.UserId;
import jakarta.servlet.http.HttpServletRequest;
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
     * @param httpRequest HTTP servlet request
     * @return ResponseEntity with list of pages with user roles
     */
    @GetMapping("/pages")
    public ResponseEntity<List<PageResponse>> getUserPages(HttpServletRequest httpRequest) {
        List<PageResponse> pages = pageService.getAllAccessiblePagesWithRoles(UserId.extract(httpRequest));
        return ResponseEntity.ok(pages);
    }

    /**
     * Get list of pages shared with the user (where user is not the owner)
     * @param httpRequest HTTP servlet request
     * @return ResponseEntity with list of shared pages
     */
    @GetMapping("/pages/shared")
    public ResponseEntity<List<PageResponse>> getSharedPages(HttpServletRequest httpRequest) {
        List<PageResponse> pages = pageService.getSharedPagesWithRoles(UserId.extract(httpRequest));
        return ResponseEntity.ok(pages);
    }

    /**
     * Create a new page
     * @param createPageRequest create page request
     * @param request HTTP servlet request
     * @return ResponseEntity with created page
     */
    @PostMapping("/pages")
    public ResponseEntity<PageContentResponse> createPage(
            @Valid @RequestBody CreatePageRequest createPageRequest,
            HttpServletRequest request
    ) {
        PageContentResponse page = pageService.createPage(
            createPageRequest.getTitle(),
            UserId.extract(request),
            createPageRequest.getFolderId()
        );
        return ResponseEntity.status(201).body(page);
    }

    /**
     * Get a page by ID
     * @param pageId page ID
     * @param request HTTP servlet request
     * @return ResponseEntity with the page
     */
    @GetMapping("/pages/{pageId}")
    public ResponseEntity<PageContentResponse> getPage(
            @PathVariable String pageId,
            HttpServletRequest request
    ) {
        PageContentResponse page = pageService.getPage(pageId, UserId.extract(request));
        return ResponseEntity.ok(page);
    }

    /**
     * Update a page's content
     * @deprecated Use the Websocket implementation for real-time updates {@link com.papairs.docs.ws.DocWebSocketHandler}
     * @param pageId page ID
     * @param updatePageRequest update page request
     * @param request HTTP servlet request
     * @return ResponseEntity with updated page
     */
    @PutMapping("/pages/{pageId}")
    public ResponseEntity<PageContentResponse> updatePage(
            @PathVariable String pageId,
            @RequestBody UpdatePageRequest updatePageRequest,
            HttpServletRequest request
    ) {
        // Sanitize HTML content to prevent XSS attacks
        String sanitizedContent = htmlSanitizer.sanitize(updatePageRequest.getContent());
        PageContentResponse updated = pageService.updatePage(pageId, UserId.extract(request), sanitizedContent);
        return ResponseEntity.ok(updated);
    }

    /**
     * Rename a page
     * @param pageId page ID
     * @param renamePageRequest rename page request
     * @param request HTTP servlet request
     * @return ResponseEntity with renamed page
     */
    @PatchMapping("/pages/{pageId}")
    public ResponseEntity<PageContentResponse> renamePage(
            @PathVariable String pageId,
            @Valid @RequestBody RenamePageRequest renamePageRequest,
            HttpServletRequest request
    ) {
        PageContentResponse renamed = pageService.renamePage(
            pageId,
            UserId.extract(request),
            renamePageRequest.getNewTitle()
        );
        return ResponseEntity.ok(renamed);
    }

    /**
     * Move a page to a new folder
     * @param pageId page ID
     * @param movePageRequest move page request
     * @param request HTTP servlet request
     * @return ResponseEntity with moved page
     */
    @PatchMapping("/pages/{pageId}/move")
    public ResponseEntity<PageContentResponse> movePage(
            @PathVariable String pageId,
            @Valid @RequestBody MovePageRequest movePageRequest,
            HttpServletRequest request
    ) {
        PageContentResponse moved = pageService.movePage(
            pageId,
            movePageRequest.getFolderId(),
            UserId.extract(request)
        );
        return ResponseEntity.ok(moved);
    }

    /**
     * Delete a page by ID
     * @param pageId page ID
     * @param request HTTP servlet request
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/pages/{pageId}")
    public ResponseEntity<Void> deletePage(
            @PathVariable String pageId,
            HttpServletRequest request
    ) {
        pageService.deletePage(pageId, UserId.extract(request));
        return ResponseEntity.noContent().build();
    }
}
