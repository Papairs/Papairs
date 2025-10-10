package com.papairs.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    
    private boolean success;
    private String message;
    private String sessionToken;
    private UserResponse user;

    public AuthResponse() {
    }

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AuthResponse(boolean success, String message, String sessionToken, UserResponse user) {
        this.success = success;
        this.message = message;
        this.sessionToken = sessionToken;
        this.user = user;
    }

    public static AuthResponse success(String message, String sessionToken, UserResponse user) {
        return new AuthResponse(true, message, sessionToken, user);
    }

    public static AuthResponse success(String message, UserResponse user) {
        return new AuthResponse(true, message, null, user);
    }
    
    public static AuthResponse success(String message) {
        return new AuthResponse(true, message);
    }
    
    public static AuthResponse error(String message) {
        return new AuthResponse(false, message);
    }

    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSessionToken() {
        return sessionToken;
    }
    
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    
    public UserResponse getUser() {
        return user;
    }
    
    public void setUser(UserResponse user) {
        this.user = user;
    }
}
