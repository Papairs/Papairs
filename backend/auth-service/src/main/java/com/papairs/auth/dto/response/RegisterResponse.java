package com.papairs.auth.dto.response;

public record RegisterResponse(
        String message,
        UserResponse user
) {}
