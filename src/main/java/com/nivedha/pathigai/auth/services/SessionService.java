package com.nivedha.pathigai.auth.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class SessionService {

    @Value("${app.session.max-concurrent-sessions:5}")
    private int maxConcurrentSessions;

    @Value("${app.session.cleanup-interval-hours:24}")
    private int cleanupIntervalHours;

    // Store active sessions for each user
    // Key: userId, Value: Map of sessionId -> SessionInfo
    private final Map<Integer, Map<String, SessionInfo>> userSessions = new ConcurrentHashMap<>();

    // Counter for generating unique session IDs
    private final AtomicInteger sessionCounter = new AtomicInteger(0);

    /**
     * Create a new session for a user
     */
    public String createSession(Integer userId, String deviceInfo) {
        String sessionId = generateSessionId();
        SessionInfo sessionInfo = new SessionInfo(sessionId, userId, deviceInfo, LocalDateTime.now());

        // Get or create user's session map
        Map<String, SessionInfo> sessions = userSessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());

        // Check if user has exceeded max concurrent sessions
        if (sessions.size() >= maxConcurrentSessions) {
            // Remove oldest session
            removeOldestSession(sessions);
            log.info("Removed oldest session for user {} due to max session limit", userId);
        }

        // Add new session
        sessions.put(sessionId, sessionInfo);
        log.info("Created new session {} for user {}", sessionId, userId);

        return sessionId;
    }

    /**
     * Validate if a session exists and is active
     */
    public boolean isValidSession(Integer userId, String sessionId) {
        Map<String, SessionInfo> sessions = userSessions.get(userId);
        if (sessions == null) {
            return false;
        }

        SessionInfo sessionInfo = sessions.get(sessionId);
        if (sessionInfo == null) {
            return false;
        }

        // Update last accessed time
        sessionInfo.setLastAccessed(LocalDateTime.now());
        return true;
    }

    /**
     * Remove a specific session
     */
    public void removeSession(Integer userId, String sessionId) {
        Map<String, SessionInfo> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            log.info("Removed session {} for user {}", sessionId, userId);

            // Clean up empty user session map
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
    }

    /**
     * Remove all sessions for a user (logout from all devices)
     */
    public void removeAllUserSessions(Integer userId) {
        Map<String, SessionInfo> sessions = userSessions.remove(userId);
        if (sessions != null) {
            log.info("Removed all {} sessions for user {}", sessions.size(), userId);
        }
    }

    /**
     * Get active session count for a user
     */
    public int getActiveSessionCount(Integer userId) {
        Map<String, SessionInfo> sessions = userSessions.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * Get all active sessions for a user
     */
    public List<SessionInfo> getUserSessions(Integer userId) {
        Map<String, SessionInfo> sessions = userSessions.get(userId);
        return sessions != null ? List.copyOf(sessions.values()) : List.of();
    }

    /**
     * Remove oldest session from user's sessions
     */
    private void removeOldestSession(Map<String, SessionInfo> sessions) {
        SessionInfo oldest = sessions.values().stream()
                .min((s1, s2) -> s1.getLastAccessed().compareTo(s2.getLastAccessed()))
                .orElse(null);

        if (oldest != null) {
            sessions.remove(oldest.getSessionId());
        }
    }

    /**
     * Generate unique session ID
     */
    private String generateSessionId() {
        return "SESSION_" + System.currentTimeMillis() + "_" + sessionCounter.incrementAndGet();
    }

    /**
     * Scheduled cleanup of expired sessions
     * Runs every hour and removes sessions older than cleanup interval
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(cleanupIntervalHours);
        int removedSessions = 0;

        for (Map.Entry<Integer, Map<String, SessionInfo>> userEntry : userSessions.entrySet()) {
            Integer userId = userEntry.getKey();
            Map<String, SessionInfo> sessions = userEntry.getValue();

            // Remove expired sessions
            List<String> expiredSessionIds = sessions.values().stream()
                    .filter(session -> session.getLastAccessed().isBefore(cutoffTime))
                    .map(SessionInfo::getSessionId)
                    .toList();

            for (String sessionId : expiredSessionIds) {
                sessions.remove(sessionId);
                removedSessions++;
            }

            // Remove empty user session maps
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }

        if (removedSessions > 0) {
            log.info("Cleaned up {} expired sessions", removedSessions);
        }
    }

    /**
     * Session information class
     */
    public static class SessionInfo {
        private final String sessionId;
        private final Integer userId;
        private final String deviceInfo;
        private final LocalDateTime createdAt;
        private LocalDateTime lastAccessed;

        public SessionInfo(String sessionId, Integer userId, String deviceInfo, LocalDateTime createdAt) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.deviceInfo = deviceInfo;
            this.createdAt = createdAt;
            this.lastAccessed = createdAt;
        }

        // Getters and setters
        public String getSessionId() { return sessionId; }
        public Integer getUserId() { return userId; }
        public String getDeviceInfo() { return deviceInfo; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastAccessed() { return lastAccessed; }
        public void setLastAccessed(LocalDateTime lastAccessed) { this.lastAccessed = lastAccessed; }
    }
}