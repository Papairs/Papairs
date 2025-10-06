package com.papairs.auth.controller;

import com.papairs.auth.dto.AuthResponse;
import com.papairs.auth.dto.LoginRequest;
import com.papairs.auth.dto.RegisterRequest;
import com.papairs.auth.dto.ApiResponse;
import com.papairs.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/health")
    public ApiResponse health() {
        return new ApiResponse("success", "Auth service is running", 
                              Map.of("timestamp", LocalDateTime.now(),
                                     "service", "auth-service",
                                     "status", "healthy"));
    }

    /**
     * Login user
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
     * Register a new user
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