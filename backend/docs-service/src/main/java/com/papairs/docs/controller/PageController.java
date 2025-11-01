package com.papairs.docs.controller;

import com.papairs.docs.dto.request.CreatePageRequest;
import com.papairs.docs.dto.request.UpdatePageRequest;
import com.papairs.docs.model.Page;
import com.papairs.docs.service.PageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "http://localhost:3000")
public class PageController {
    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    /**
     * Get user ID from request header: X-User-Id
     * @param request HTTP servlet request
     * @return User ID
     */
    private String getUserId(HttpServletRequest request) {
        return request.getHeader("X-User-Id");
    }

    /**
     * Create a new page
     * @param request create page request
     * @param httpRequest HTTP servlet request
     * @return ResponseEntity with created page
     */
    @PostMapping("/pages")
    public ResponseEntity<Page> createPage(
            @RequestBody CreatePageRequest request,
            HttpServletRequest httpRequest
    ) {
        String userId = getUserId(httpRequest);
        Page page = pageService.createPage(request.getTitle(), userId);
        return ResponseEntity.status(201).body(page);
    }

    /**
     * Get a page by ID
     * @param pageId page ID
     * @return ResponseEntity with the page
     */
    @GetMapping("/pages/{pageId}")
    public ResponseEntity<Page> getPage(@PathVariable String pageId) {
        return ResponseEntity.ok(pageService.getPage(pageId));
    }

    /**
     * Update a page's content
     * @param pageId page ID
     * @param request update page request
     * @return ResponseEntity with updated page
     */
    @PutMapping("/pages/{pageId}")
    public ResponseEntity<Page> updatePage(
            @PathVariable String pageId,
            @RequestBody UpdatePageRequest request
    ) {
        Page updated = pageService.updatePage(pageId, request.getContent());
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a page by ID
     * @param pageId page ID
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/pages/{pageId}")
    public ResponseEntity<Void> deletePage(@PathVariable String pageId) {
        pageService.deletePage(pageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pages/{pageId}/members")
    public String getPageMembers() {
        return "List of page members";
    }
}
