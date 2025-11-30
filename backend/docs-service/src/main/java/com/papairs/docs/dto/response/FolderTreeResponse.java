package com.papairs.docs.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FolderTreeResponse(
        String folderId,
        String name,
        LocalDateTime createdAt,
        long pageCount,
        long childCount,
        List<FolderTreeResponse> children,
        List<NavigationItemResponse> documents
) {}
