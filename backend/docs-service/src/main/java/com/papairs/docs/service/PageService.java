package com.papairs.docs.service;

import com.papairs.docs.model.Page;
import com.papairs.docs.repository.PageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PageService {
    private final PageRepository pageRepository;

    public PageService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    /**
     * Create a new page
     * @param title page title
     * @param ownerId owner's user ID
     * @return Created Page entity
     */
    public Page createPage(String title, String ownerId) {
        Page page = new Page();
        page.setPageId(UUID.randomUUID().toString());
        page.setTitle(title);
        page.setOwnerId(ownerId);
        page.setContent("");
        return pageRepository.save(page);
    }

    /**
     * Update page content
     * @param pageId page ID
     * @param content new content
     * @return Updated Page entity
     */
    public Page updatePage(String pageId, String content) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Page not found"));
        page.setContent(content);
        return pageRepository.save(page);
    }

    /**
     * Get a page by ID
     * @param pageId page ID
     * @return Page entity
     */
    public Page getPage(String pageId) {
        return pageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Page not found"));
    }

    /**
     * Get all pages for a user
     * @param userId user ID
     * @return List of Page entities
     */
    public List<Page> getUserPages(String userId) {
        return pageRepository.findByOwnerId(userId);
    }

    /**
     * Delete a page by ID
     * @param pageId page ID
     */
    public void deletePage(String pageId) {
        pageRepository.deleteById(pageId);
    }
}
