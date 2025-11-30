package com.papairs.orchestration.dto.response;

public record RegisterResponse(
        String message,
        UserResponse user
) {}
