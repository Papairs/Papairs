package com.papairs.auth.dto.request;

public class LogoutRequest extends TokenRequest {
    public LogoutRequest() {
        super();
    }

    public LogoutRequest(String token) {
        super(token);
    }
}
