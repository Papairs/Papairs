package com.papairs.auth.service;

import com.papairs.auth.dto.UserDto;
import com.papairs.auth.model.User;
import com.papairs.auth.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Create a new user with hashed password
     * @param email user email
     * @param password unhashed password
     * @return created User
     */
    @Transactional
    public User createUser(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmailVerified(false);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    /**
     * Find user by email
     * @param email email address
     * @return Optional<User> if found, else empty
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by ID
     * @param id user ID
     * @return Optional<User> if found, else empty
     */
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    /**
     * Check if email exists
     * @param email email address
     * @return true if exists, else false
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Check if user account is active
     * @param user User entity
     * @return true if active, else false
     */
    public boolean isUserActive(User user) {
        return user.getIsActive();
    }

    /**
     * Verify plain password against hashed password
     * @param plainPassword unhashed password
     * @param hashedPassword hashed password
     * @return true if matches, else false
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    /**
     * Update user's last login timestamp
     * @param userId user ID
     */
    @Transactional
    public void updateLastLogin(String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        userOpt.ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * Convert User entity to UserDto
     * @param user User entity
     * @return UserDto
     */
    public UserDto toDto(User user) {
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
