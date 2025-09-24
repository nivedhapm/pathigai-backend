package com.nivedha.pathigai.auth.services;

import com.nivedha.pathigai.auth.repositories.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupService {

    private final SessionRepository sessionRepository;

    /**
     * Clean up expired sessions every hour
     * Removes sessions where refresh token has expired
     */
    @Scheduled(fixedRate = 3600000) // Every hour (3,600,000 ms)
    @Transactional
    public void cleanupExpiredSessions() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Find sessions that are expired but still marked as active
            int deactivatedCount = sessionRepository.deactivateExpiredSessions(now);

            if (deactivatedCount > 0) {
                log.info("🧹 SESSION CLEANUP: Deactivated {} expired sessions", deactivatedCount);
            }

            // Optionally delete very old inactive sessions (older than 7 days)
            int deletedCount = sessionRepository.deleteOldInactiveSessions(now.minusDays(7));

            if (deletedCount > 0) {
                log.info("🗑️ SESSION CLEANUP: Deleted {} old inactive sessions", deletedCount);
            }

        } catch (Exception e) {
            log.error("❌ SESSION CLEANUP FAILED: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual cleanup method for testing
     */
    @Transactional
    public void cleanupNow() {
        log.info("🧹 MANUAL SESSION CLEANUP TRIGGERED");
        cleanupExpiredSessions();
    }
}
