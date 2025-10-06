package com.papairs.auth.service;

import com.papairs.auth.dto.AuthResponse;
import com.papairs.auth.dto.LoginRequest;
import com.papairs.auth.dto.RegisterRequest;
import com.papairs.auth.dto.UserDto;
import com.papairs.auth.model.User;
import com.papairs.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * Register a new user
     */
    public AuthResponse register(RegisterRequest request) {
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                return AuthResponse.error("Email already registered");
            }
            
            // Create new user
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setEmailVerified(false);
            user.setIsActive(true);
            
            // Save user to database
            User savedUser = userRepository.save(user);
            
            // Convert to DTO
            UserDto userDto = convertToDto(savedUser);
            
            return AuthResponse.success("User registered successfully", null, userDto);
            
        } catch (Exception e) {
            return AuthResponse.error("Registration failed: " + e.getMessage());
        }
    }
    
    /**
     * Authenticate user login
     */
    public AuthResponse login(LoginRequest request) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
            
            if (userOpt.isEmpty()) {
                return AuthResponse.error("Invalid email or password");
            }
            
            User user = userOpt.get();

            if (!user.getIsActive()) {
                return AuthResponse.error("Account is deactivated");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                return AuthResponse.error("Invalid email or password");
            }
            
            // Update last login time
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            
            // Generate session token (simple UUID for now)
            String sessionToken = UUID.randomUUID().toString();
            
            // Convert to DTO
            UserDto userDto = convertToDto(user);
            
            return AuthResponse.success("Login successful", sessionToken, userDto);
            
        } catch (Exception e) {
            return AuthResponse.error("Login failed: " + e.getMessage());
        }
    }
    
    /**
     * Convert User entity to UserDto
     */
    private UserDto convertToDto(User user) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getEmailVerified(),
            user.getIsActive(),
            user.getCreatedAt(),
            user.getLastLoginAt()
        );
    }
}
