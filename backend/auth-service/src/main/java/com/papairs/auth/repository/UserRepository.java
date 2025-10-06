package com.papairs.auth.repository;

import com.papairs.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * Find user by email address
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if email exists in database
     */
    boolean existsByEmail(String email);
    
    /**
     * Find all active users
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    java.util.List<User> findAllActiveUsers();
    
    /**
     * Find users by email verification status
     */
    @Query("SELECT u FROM User u WHERE u.emailVerified = :verified")
    java.util.List<User> findByEmailVerified(@Param("verified") Boolean verified);
    
    /**
     * Update last login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    int updateLastLoginAt(@Param("userId") String userId, @Param("loginTime") LocalDateTime loginTime);
    
    /**
     * Activate/Deactivate user account
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = :active WHERE u.id = :userId")
    int updateUserActiveStatus(@Param("userId") String userId, @Param("active") Boolean active);
    
    /**
     * Mark email as verified
     */
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.id = :userId")
    int markEmailAsVerified(@Param("userId") String userId);
    
    /**
     * Update user password hash
     */
    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.id = :userId")
    int updatePasswordHash(@Param("userId") String userId, @Param("passwordHash") String passwordHash);
}
