package com.papairs.auth.dto.response;

import com.papairs.auth.model.Session;
import com.papairs.auth.model.User;

public class LoginResponse {
    private Session session;
    private User user;

    public LoginResponse(Session session, User user) {
        this.session = session;
        this.user = user;
    }

    public Session getSession() {
        return session;
    }

    public User getUser() {
        return user;
    }
}