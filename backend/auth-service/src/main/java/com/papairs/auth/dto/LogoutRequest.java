package com.papairs.auth.dto;

public class LogoutRequest extends TokenRequest {
    public LogoutRequest() {
        super();
    }

    public LogoutRequest(String token) {
        super(token);
    }
}
