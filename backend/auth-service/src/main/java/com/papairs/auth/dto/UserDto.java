package com.papairs.auth.dto;

import java.time.LocalDateTime;

public class UserDto {
    
    private String id;
    private String email;
    private Boolean emailVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public UserDto() {
    }

    public UserDto(String id, String email, Boolean emailVerified, Boolean isActive,
                   LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        this.id = id;
        this.email = email;
        this.emailVerified = emailVerified;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Boolean getEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
