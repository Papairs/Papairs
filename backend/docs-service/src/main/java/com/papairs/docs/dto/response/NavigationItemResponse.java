package com.papairs.docs.dto.response;

import com.papairs.docs.model.Page;

public record NavigationItemResponse(String pageId, String title) {
    public static NavigationItemResponse from(Page page) {
        return new NavigationItemResponse(page.getPageId(), page.getTitle());
    }
}
