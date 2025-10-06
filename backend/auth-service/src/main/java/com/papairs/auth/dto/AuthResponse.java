package com.papairs.auth.dto;

public class AuthResponse {
    
    private boolean success;
    private String message;
    private String sessionToken;
    private UserDto user;

    public AuthResponse() {
    }

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AuthResponse(boolean success, String message, String sessionToken, UserDto user) {
        this.success = success;
        this.message = message;
        this.sessionToken = sessionToken;
        this.user = user;
    }

    public static AuthResponse success(String message, String sessionToken, UserDto user) {
        return new AuthResponse(true, message, sessionToken, user);
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
    
    public UserDto getUser() {
        return user;
    }
    
    public void setUser(UserDto user) {
        this.user = user;
    }
}
