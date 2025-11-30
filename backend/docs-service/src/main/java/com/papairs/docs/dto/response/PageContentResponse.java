package com.papairs.docs.dto.response;

import com.papairs.docs.model.Page;

import java.time.LocalDateTime;

public record PageContentResponse(
        String pageId,
        String title,
        String folderId,
        String ownerId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PageContentResponse of(Page page) {
        return new PageContentResponse(
                page.getPageId(),
                page.getTitle(),
                page.getFolderId(),
                page.getOwnerId(),
                page.getContent(),
                page.getCreatedAt(),
                page.getUpdatedAt()
        );
    }
}
