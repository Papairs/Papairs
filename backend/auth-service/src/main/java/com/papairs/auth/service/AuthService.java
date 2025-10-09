package com.papairs.auth.service;

import com.papairs.auth.dto.*;
import com.papairs.auth.model.Session;
import com.papairs.auth.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
     * @return AuthResponse with user details or error message
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        try {
            if (userService.emailExists(request.getEmail())) {
                return AuthResponse.error("Email already registered");
            }

            User user = userService.createUser(request.getEmail(), request.getPassword());

            UserDto userDto = userService.toDto(user);

            return AuthResponse.success("User registered successfully", null, userDto);
            
        } catch (Exception e) {
            return AuthResponse.error("Registration failed: " + e.getMessage());
        }
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
     * @return AuthResponse with session token and user details or error message
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Optional<User> userOpt = userService.findByEmail(request.getEmail());

            if (userOpt.isEmpty()) {
                return AuthResponse.error("Invalid email or password");
            }
            
            User user = userOpt.get();

            if (!userService.isUserActive(user)) {
                return AuthResponse.error("Account is deactivated");
            }

            if (!userService.verifyPassword(request.getPassword(), user.getPasswordHash())) {
                return AuthResponse.error("Invalid email or password");
            }

            userService.updateLastLogin(user.getId());

            Session session = sessionService.createSession(user.getId());

            UserDto userDto = userService.toDto(user);
            
            return AuthResponse.success("Login successful", session.getToken(), userDto);
            
        } catch (Exception e) {
            return AuthResponse.error("Login failed: " + e.getMessage());
        }
    }

    /**
     * Logout user by deleting session
     * @param token session token
     * @return AuthResponse indicating success or failure
     */
    @Transactional
    public AuthResponse logout(String token) {
        try {
            sessionService.deleteByToken(token);
            return AuthResponse.success("Logout successful", null, null);
        } catch (Exception e) {
            return AuthResponse.error("Logout failed: " + e.getMessage());
        }
    }

    /**
     * Validate session and return user information
     *
     * Firstly, check if session exists
     * Check if session is expired, if so delete it
     * Check if user exists and is active, if not delete session
     * Lastly, update session last active timestamp
     *
     * TODO: Use try-catch to handle unexpected errors
     *
     * @param token session token
     * @return UserDto if valid, null if invalid
     */
    @Transactional
    public UserDto validateSession(String token) {
        Optional<Session> sessionOpt = sessionService.findByToken(token);

        if (sessionOpt.isEmpty()) {
            return null;
        }

        Session session = sessionOpt.get();

        if (sessionService.isExpired(session)) {
            sessionService.delete(session);
            return null;
        }

        Optional<User> userOpt = userService.findById(session.getUserId());

        if (userOpt.isEmpty()) {
            sessionService.delete(session);
            return null;
        }

        User user = userOpt.get();

        if (!userService.isUserActive(user)) {
            return null;
        }

        sessionService.updateLastActive(session);

        return userService.toDto(user);
    }
}
