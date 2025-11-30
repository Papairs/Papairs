package com.papairs.docs.dto.response;

import com.papairs.docs.model.Page;
import com.papairs.docs.model.enums.MemberRole;

import java.time.LocalDateTime;

public record PageResponse(
        String pageId,
        String title,
        String folderId,
        String ownerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String userRole
) {
    public static PageResponse of(Page page, String userId, MemberRole memberRole) {
        String role;
        if (page.getOwnerId().equals(userId)) {
            role = "OWNER";
        } else if (memberRole != null) {
            role = memberRole.name();
        } else {
            role = null;
        }

        return new PageResponse(
                page.getPageId(),
                page.getTitle(),
                page.getFolderId(),
                page.getOwnerId(),
                page.getCreatedAt(),
                page.getUpdatedAt(),
                role
        );
    }

    public static PageResponse from(Page page) {
        return new PageResponse(
                page.getPageId(),
                page.getTitle(),
                page.getFolderId(),
                page.getOwnerId(),
                page.getCreatedAt(),
                page.getUpdatedAt(),
                null
        );
    }
}
