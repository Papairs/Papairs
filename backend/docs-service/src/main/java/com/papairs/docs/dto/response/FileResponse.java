package com.papairs.docs.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.papairs.docs.model.UserFile;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileResponse(
        String fileId,
        String filename,
        Long fileSize,
        String mimeType,
        LocalDateTime createdAt
) {
    public static FileResponse of(UserFile file) {
        return new FileResponse(
                file.getFileId(),
                file.getFilename(),
                file.getFileSize(),
                file.getMimeType(),
                file.getCreatedAt()
        );
    }
}
