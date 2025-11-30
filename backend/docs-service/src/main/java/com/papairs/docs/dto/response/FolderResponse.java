package com.papairs.docs.dto.response;

import com.papairs.docs.model.Folder;

import java.time.LocalDateTime;

public record FolderResponse(
        String folderId,
        String name,
        String parentFolderId,
        String ownerId,
        LocalDateTime createdAt
) {
    public static FolderResponse of(Folder folder) {
        return new FolderResponse(
                folder.getFolderId(),
                folder.getName(),
                folder.getParentFolderId(),
                folder.getOwnerId(),
                folder.getCreatedAt()
        );
    }
}
