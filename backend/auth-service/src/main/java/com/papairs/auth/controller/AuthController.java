package com.papairs.auth.controller;

import com.papairs.auth.model.LoginRequest;
import com.papairs.auth.model.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @GetMapping("/health")
    public ApiResponse health() {
        return new ApiResponse("success", "Auth service is running", 
                              Map.of("timestamp", LocalDateTime.now(),
                                     "service", "auth-service",
                                     "status", "healthy"));
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody LoginRequest loginRequest) {
        // Simple mock authentication
        if ("test".equals(loginRequest.getUsername()) && "test".equals(loginRequest.getPassword())) {
            return new ApiResponse("success", "Login successful", 
                                  Map.of("token", "mock-jwt-token-12345",
                                         "user", loginRequest.getUsername(),
                                         "timestamp", LocalDateTime.now()));
        } else {
            return new ApiResponse("error", "Invalid credentials", null);
        }
    }

    @PostMapping("/register")
    public ApiResponse register(@RequestBody LoginRequest registerRequest) {
        // Simple mock registration
        return new ApiResponse("success", "User registered successfully", 
                              Map.of("user", registerRequest.getUsername(),
                                     "timestamp", LocalDateTime.now()));
    }

    @GetMapping("/validate")
    public ApiResponse validateToken(@RequestHeader("Authorization") String token) {
        // Simple mock token validation
        if (token != null && token.startsWith("Bearer ")) {
            return new ApiResponse("success", "Token is valid", 
                                  Map.of("valid", true,
                                         "timestamp", LocalDateTime.now()));
        } else {
            return new ApiResponse("error", "Invalid token", 
                                  Map.of("valid", false));
        }
    }
}