package com.papairs.auth.dto.response;

import java.time.LocalDateTime;

public record LoginResponse(
        String token,
        String sessionId,
        LocalDateTime expiresAt,
        UserResponse user
) {}