package com.papairs.auth.controller;

import com.papairs.auth.dto.request.LoginRequest;
import com.papairs.auth.dto.request.RegisterRequest;
import com.papairs.auth.dto.response.ApiResponse;
import com.papairs.auth.dto.response.AuthResponse;
import com.papairs.auth.dto.response.UserResponse;
import com.papairs.auth.exception.InvalidAuthHeaderException;
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
        AuthResponse response = authService.login(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * Logout user by invalidating session
     * Requires Authorization header: Bearer <token>
     * @param authHeader Authorization header containing Bearer token
     * @return ApiResponse indicating success or failure
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractBearerToken(authHeader);
            AuthResponse response = authService.logout(token);

            if (response.isSuccess()) {
                return ResponseEntity.ok(new ApiResponse("success", response.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse("error", response.getMessage()));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("error", e.getMessage()));
        }
    }

    /**
     * Register a new user
     * @param request registration request
     * @return AuthResponse with user details or error message
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);

        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Validate session token and return user information
     * Requires Authorization header: Bearer <token>
     * @param authHeader session token from Authorization header
     * @return ApiResponse indicating if token is valid or not
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractBearerToken(authHeader);
            UserResponse user = authService.validateSession(token);

            if (user != null) {
                return ResponseEntity.ok(
                        new ApiResponse("success", "Token is valid", user)
                );
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse("error", "Invalid or expired token"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("error", "Invalid token format"));
        }
    }

    /**
     * Extract Bearer token from Authorization header
     * @param authHeader Authorization header value
     * @return extracted token
     * @throws IllegalArgumentException if header is invalid
     * */
    private String extractBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is missing");
            throw new InvalidAuthHeaderException("Authorization header is missing");
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
            throw new InvalidAuthHeaderException("Authorization header must start with 'Bearer '");
        }

        String token = authHeader.substring(7).trim();

        if (token.isBlank()) {
            throw new IllegalArgumentException("Token is empty");
            throw new InvalidAuthHeaderException("Token is empty");
        }

        return token;
    }
}