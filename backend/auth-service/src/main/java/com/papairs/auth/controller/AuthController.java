package com.papairs.auth.controller;

import com.papairs.auth.dto.request.LoginRequest;
import com.papairs.auth.dto.request.RegisterRequest;
import com.papairs.auth.dto.response.ApiResponse;
import com.papairs.auth.dto.response.AuthResponse;
import com.papairs.auth.dto.response.LoginResponse;
import com.papairs.auth.dto.response.UserResponse;
import com.papairs.auth.exception.InvalidAuthHeaderException;
import com.papairs.auth.model.User;
import com.papairs.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/health")
    public ApiResponse health() {
        return new ApiResponse("success", "Auth service is running", 
                              Map.of("timestamp", LocalDateTime.now(),
                                     "service", "auth-service",
                                     "status", "healthy"));
    }

    /**
     * Login user
     * @param request login request
     * @return AuthResponse with user details and token or error message
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginDto = authService.login(request);
        UserResponse userDto = toDto(loginDto.getUser());

        return ResponseEntity.ok(
                AuthResponse.success("Login successful",
                        loginDto.getSession().getToken(), userDto)
        );
    }

    /**
     * Logout user by invalidating session
     * Requires Authorization header: Bearer <token>
     * @param authHeader Authorization header containing Bearer token
     * @return AuthResponse indicating success or failure
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearerToken(authHeader);
        authService.logout(token);

        return ResponseEntity.ok(
                AuthResponse.success("Logout successful")
        );
    }

    /**
     * Register a new user
     * @param request registration request
     * @return AuthResponse with user details or error message
     * Does not return a sessionToken
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        UserResponse userDto = toDto(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthResponse.success("User registered successfully", userDto)
        );
    }

    /**
     * Validate session token and return user information
     * Requires Authorization header: Bearer <token>
     * @param authHeader session token from Authorization header
     * @return AuthResponse indicating if token is valid or not
     */
    @PostMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearerToken(authHeader);
        User user = authService.validateSession(token);

        return ResponseEntity.ok(
                AuthResponse.success("Token is valid", toDto(user))
        );
    }

    /**
     * Extract Bearer token from Authorization header
     * @param authHeader Authorization header value
     * @return extracted token
     * */
    private String extractBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new InvalidAuthHeaderException("Authorization header is missing");
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new InvalidAuthHeaderException("Authorization header must start with 'Bearer '");
        }

        String token = authHeader.substring(7).trim();

        if (token.isBlank()) {
            throw new InvalidAuthHeaderException("Token is empty");
        }

        return token;
    }

    /**
     * Convert User entity to UserDto
     * @param user User entity
     * @return UserResponse
     */
    public UserResponse toDto(User user) {
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