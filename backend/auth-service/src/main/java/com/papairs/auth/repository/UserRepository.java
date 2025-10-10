package com.papairs.auth.repository;

import com.papairs.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by email address
     * @param email email address
     * @return Optional<User> if found, else empty
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email exists in database
     * @param email email address
     * @return true if exists, else false
     */
    boolean existsByEmail(String email);

    /**
     * Find all active users
     * @return List of active users
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    List<User> findAllActiveUsers();

    /**
     * Find users by email verification status
     * @param verified email with verified status
     * @return List of users with the given email verification status
     */
    @Query("SELECT u FROM User u WHERE u.emailVerified = :verified")
    List<User> findByEmailVerified(@Param("verified") Boolean verified);

    /**
     * Update last login timestamp
     * @param userId user ID
     * @param loginTime login timestamp
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    int updateLastLoginAt(@Param("userId") String userId, @Param("loginTime") LocalDateTime loginTime);

    /**
     * Activate/Deactivate user account
     * @param userId user ID
     * @param active active status
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = :active WHERE u.id = :userId")
    int updateUserActiveStatus(@Param("userId") String userId, @Param("active") Boolean active);

    /**
     * Mark email as verified
     * @param userId user ID
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.id = :userId")
    int markEmailAsVerified(@Param("userId") String userId);

    /**
     * Update user password hash
     * @param userId user ID
     * @param passwordHash new password hash
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.id = :userId")
    int updatePasswordHash(@Param("userId") String userId, @Param("passwordHash") String passwordHash);
}
