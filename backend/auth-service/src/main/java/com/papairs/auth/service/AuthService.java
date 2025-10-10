package com.papairs.auth.service;

import com.papairs.auth.dto.request.LoginRequest;
import com.papairs.auth.dto.request.RegisterRequest;
import com.papairs.auth.dto.response.LoginResponse;
import com.papairs.auth.exception.AuthenticationException;
import com.papairs.auth.exception.InvalidTokenException;
import com.papairs.auth.exception.UserAlreadyExistsException;
import com.papairs.auth.exception.UserDeactivatedException;
import com.papairs.auth.model.Session;
import com.papairs.auth.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserService userService;
    private final SessionService sessionService;

    public AuthService(SessionService sessionService, UserService userService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    /**
     * Register a new user
     * @param request registration request
     * @return User entity if successful, else throw exception
     */
    @Transactional
    public User register(RegisterRequest request) {
        if (userService.emailExists(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        return userService.createUser(request.getEmail(), request.getPassword())
                .orElseThrow(() -> new AuthenticationException("Failed to create user"));
    }

    /**
     * Authenticate user login
     *
     * Firstly, check if user exists
     * Check if user is active
     * Check if password matches
     * Update last login timestamp
     * Create session and return token
     *
     * @param request login request
     * @return LoginResponse with session token and user details, else throw exception
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!userService.isUserActive(user)) {
            throw new UserDeactivatedException("Account is deactivated");
        }

        if (!userService.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        Session session = sessionService.createSession(user.getId());

        userService.updateLastLogin(user.getId());

        return new LoginResponse(session, user);
    }

    /**
     * Logout user by deleting session
     * @param token session token
     * Does not throw error if session not found for security reasons
     */
    @Transactional
    public void logout(String token) {
        sessionService.deleteByToken(token);
    }

    /**
     * Validate session and return user information
     *
     * Firstly, check if session exists
     * Check if session is expired, if so delete it
     * Check if user exists
     * Check if user is active, if not delete session
     * Lastly, update session last active timestamp
     *
     * @param token session token
     * @return User entity if valid, else throw exception
     */
    @Transactional
    public User validateSession(String token) {
        Session session = sessionService.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid session token"));

        if (sessionService.isExpired(session)) {
            sessionService.delete(session);
            throw new InvalidTokenException("Session expired");
        }

        User user = userService.findById(session.getUserId())
                .orElseThrow(() -> new AuthenticationException("User not found for session"));

        if (!userService.isUserActive(user)) {
            sessionService.delete(session);
            throw new UserDeactivatedException("User account is deactivated");
        }

        sessionService.updateLastActive(session);

        return user;
    }
}
