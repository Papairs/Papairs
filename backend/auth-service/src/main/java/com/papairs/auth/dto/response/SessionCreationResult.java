package com.papairs.auth.dto.response;

import java.time.LocalDateTime;

public record SessionCreationResult(
        String sessionId,
        String userId,
        String token,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {}
