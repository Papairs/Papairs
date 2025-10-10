package com.papairs.auth.repository;

import com.papairs.auth.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {

    /**
     * Find session by token
     * @param token session token
     * @return Optional<Session> if found, else empty
     */
    Optional<Session> findByToken(String token);

    /**
     * Check if a session with the given token exists
     * @param token session token
     * @return true if exists, else false
     */
    boolean existsByToken(String token);

    /**
     * Find all sessions for a given user ID
     * @param userId user ID
     * @return List of sessions for the user
     */
    @Query("SELECT s FROM Session s WHERE s.userId = :userId")
    List<Session> findByUserId(String userId);

    /** Delete a session by its token
     * @param token session token
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Session s WHERE s.token = :token")
    void deleteByToken(String token);

    /**
     * Delete all sessions for a given user ID
     * @param userId user ID
     * @return number of rows affected
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Session s WHERE s.userId = :userId")
    int deleteByUserId(String userId);

    /**
     * Delete all expired sessions
     * @param currentTime current timestamp
     * @return number of rows affected
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Session s WHERE s.expiresAt < :currentTime")
    int deleteByExpiresAtBefore(LocalDateTime currentTime);
}
