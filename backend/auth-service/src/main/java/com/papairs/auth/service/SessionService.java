package com.papairs.auth.service;

import com.papairs.auth.model.Session;
import com.papairs.auth.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    @Value("${session.duration.hours:24}")
    private int sessionDurationHours;
    private final SessionRepository sessionRepository;
    private final SecureRandom secureRandom;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Create a new session for a user
     * @param userId user ID
     * @return created Session
     */
    @Transactional
    public Session createSession(String userId) {
        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setToken(generateSecureToken());
        session.setExpiresAt(LocalDateTime.now().plusHours(sessionDurationHours));
        session.setCreatedAt(LocalDateTime.now());
        session.setLastActiveAt(LocalDateTime.now());

        sessionRepository.save(session);

        return session;
    }

    /**
     * Find a session by its token
     * @param token session token
     * @return Optional<Session> if found, else empty
     */
    public Optional<Session> findByToken(String token) {
        return sessionRepository.findByToken(token);
    }

    /**
     * Update the last active timestamp of a session
     * @param session Session entity
     */
    @Transactional
    public void updateLastActive(Session session) {
        // Only update if > 5 minutes old
        if (session.getLastActiveAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            session.setLastActiveAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }

    /**
     * Check if a session is expired
     * @param session Session entity
     * @return true if expired, else false
     */
    public boolean isExpired(Session session) {
        return session.getExpiresAt().isBefore(LocalDateTime.now());
    }

    /**
     * Delete a session by token (logout)
     * @param token session token
     */
    @Transactional
    public void deleteByToken(String token) {
        sessionRepository.deleteByToken(token);
    }

    /**
     * Delete a specific session entity
     * @param session Session entity
     */
    @Transactional
    public void delete(Session session) {
        sessionRepository.delete(session);
    }

    /**
     * Delete all user sessions
     * TODO: Consider security implications
     * @param userId user ID
     * @return number of sessions deleted
     */
    public int deleteAllUserSessions(String userId) {
        return sessionRepository.deleteByUserId(userId);
    }

    /**
     * Delete all expired sessions (clean-up)
     */
    public int deleteExpiredSessions() {
        return sessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    /**
     * Generate a cryptographically secure random token
     * @return secure random token string
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
