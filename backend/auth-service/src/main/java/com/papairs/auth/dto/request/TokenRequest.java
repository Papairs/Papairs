package com.papairs.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public class TokenRequest {

    @NotBlank(message = "Session token is required")
    private String token;

    public TokenRequest() {
    }

    public TokenRequest(String sessionToken) {
        this.token = sessionToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
