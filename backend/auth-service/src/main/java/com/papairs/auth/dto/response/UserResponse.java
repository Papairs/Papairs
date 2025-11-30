package com.papairs.auth.dto.response;

import com.papairs.auth.model.User;

import java.time.LocalDateTime;

public record UserResponse(
        String id,
        String email,
        boolean emailVerified,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getEmailVerified(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}