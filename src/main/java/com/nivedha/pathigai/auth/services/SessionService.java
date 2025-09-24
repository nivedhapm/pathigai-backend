package com.nivedha.pathigai.auth.services;

import com.nivedha.pathigai.auth.entities.Session;
import com.nivedha.pathigai.auth.repositories.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionManagementService sessionManagementService;

    @Value("${app.session.max-concurrent-sessions:3}")
    private int maxConcurrentSessions;

    @Value("${app.session.cleanup-interval-hours:1}")
    private int cleanupIntervalHours;

    /**
     * Create a new session or reuse existing one for the same device
     */
    @Transactional
    public Session createOrReuseSession(Integer userId, String accessToken, String refreshToken,
                                       LocalDateTime accessExpiresAt, LocalDateTime refreshExpiresAt,
                                       String ipAddress, String userAgent) {

        // Use the enhanced session management service
        SessionManagementService.SessionRequest request = SessionManagementService.SessionRequest.builder()
            .userId(userId)
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .accessExpiresAt(accessExpiresAt)
            .refreshExpiresAt(refreshExpiresAt)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

        SessionManagementService.SessionResult result = sessionManagementService.createOrReuseSession(request);

        if (result.isSuccess()) {
            Optional<Session> sessionOpt = sessionRepository.findById(result.getSessionId());
            if (sessionOpt.isPresent()) {
                log.info("Session {} for user {} - Action: {}",
                    result.getSessionId(), userId, result.getAction());
                return sessionOpt.get();
            }
        }

        throw new RuntimeException("Failed to create or reuse session: " + result.getErrorMessage());
    }

    /**
     * Find session by refresh token
     */
    public Optional<Session> findByRefreshToken(String refreshToken) {
        String hashedToken = sessionManagementService.hashToken(refreshToken);
        return sessionRepository.findByRefreshTokenHashAndIsActiveTrueAndRefreshExpiresAtAfter(
            hashedToken, LocalDateTime.now());
    }

    /**
     * Find session by access token
     */
    public Optional<Session> findByAccessToken(String accessToken) {
        String hashedToken = sessionManagementService.hashToken(accessToken);
        return sessionRepository.findByAccessTokenHashAndIsActiveTrueAndAccessExpiresAtAfter(
            hashedToken, LocalDateTime.now());
    }

    /**
     * Refresh session tokens
     */
    @Transactional
    public Session refreshSession(String oldRefreshToken, String newAccessToken, String newRefreshToken,
                                 LocalDateTime newAccessExpiresAt, LocalDateTime newRefreshExpiresAt,
                                 String ipAddress) {

        SessionManagementService.RefreshTokenResult result = sessionManagementService.refreshToken(
            oldRefreshToken, newAccessToken, newRefreshToken,
            newAccessExpiresAt, newRefreshExpiresAt, ipAddress);

        if (result.isSuccess()) {
            Optional<Session> sessionOpt = sessionRepository.findById(result.getSessionId());
            if (sessionOpt.isPresent()) {
                log.info("Refreshed session {} for user {}", result.getSessionId(), result.getUserId());
                return sessionOpt.get();
            }
        }

        throw new RuntimeException("Failed to refresh session: " + result.getErrorMessage());
    }

    /**
     * Validate if a session exists and is active
     */
    public boolean isValidSession(Integer userId, String token, boolean isRefreshToken) {
        try {
            if (isRefreshToken) {
                return findByRefreshToken(token).isPresent();
            } else {
                return findByAccessToken(token).isPresent();
            }
        } catch (Exception e) {
            log.error("Error validating session for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Remove a specific session by ID
     */
    @Transactional
    public void removeSession(Integer sessionId, String reason) {
        sessionRepository.deactivateSessionById(sessionId, LocalDateTime.now(),
            Session.RevokeReason.valueOf(reason));
        log.info("Removed session {} due to: {}", sessionId, reason);
    }

    /**
     * Remove session by refresh token
     */
    @Transactional
    public void removeSessionByRefreshToken(String refreshToken, String reason) {
        String hashedToken = sessionManagementService.hashToken(refreshToken);
        int updated = sessionRepository.deactivateSessionByRefreshToken(hashedToken,
            LocalDateTime.now(), Session.RevokeReason.valueOf(reason));

        if (updated > 0) {
            log.info("Removed session by refresh token due to: {}", reason);
        }
    }

    /**
     * Remove all sessions for a user (logout from all devices)
     */
    @Transactional
    public void removeAllUserSessions(Integer userId, String reason) {
        SessionManagementService.RevokeSessionsResult result =
            sessionManagementService.revokeAllUserSessions(userId, reason);

        if (result.isSuccess()) {
            log.info("Removed all {} sessions for user {} due to: {}",
                result.getRevokedCount(), userId, reason);
        } else {
            log.error("Failed to remove sessions for user {}: {}", userId, result.getErrorMessage());
        }
    }

    /**
     * Get active session count for a user
     */
    public int getActiveSessionCount(Integer userId) {
        Long count = sessionRepository.countActiveSessionsByUserId(userId, LocalDateTime.now());
        return count != null ? count.intValue() : 0;
    }

    /**
     * Get all active sessions for a user
     */
    public List<Session> getUserActiveSessions(Integer userId) {
        return sessionRepository.findActiveSessionsByUserId(userId, LocalDateTime.now());
    }

    /**
     * Update session last used timestamp
     */
    @Transactional
    public void updateSessionActivity(Integer sessionId) {
        sessionRepository.updateLastUsedAt(sessionId, LocalDateTime.now());
    }

    /**
     * Check if user has reached max session limit
     */
    public boolean hasReachedMaxSessions(Integer userId) {
        return getActiveSessionCount(userId) >= maxConcurrentSessions;
    }

    /**
     * Find sessions by device fingerprint (for security analysis)
     */
    public List<Session> getSessionsByDeviceFingerprint(String deviceFingerprint) {
        return sessionRepository.findActiveSessionsByDeviceFingerprint(deviceFingerprint);
    }

    /**
     * Find sessions by IP address (for security analysis)
     */
    public List<Session> getSessionsByIpAddress(String ipAddress) {
        return sessionRepository.findActiveSessionsByIpAddress(ipAddress, LocalDateTime.now());
    }

    /**
     * Validate refresh token exists and is active
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        String hashedToken = sessionManagementService.hashToken(refreshToken);
        return sessionRepository.existsByRefreshTokenHashAndActive(hashedToken, LocalDateTime.now());
    }

    /**
     * Scheduled cleanup of expired sessions - runs every hour
     */
    @Scheduled(fixedRateString = "#{${app.session.cleanup-interval-hours:1} * 3600000}")
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();

        // Mark expired sessions as inactive
        int markedInactive = sessionRepository.markExpiredSessionsAsInactive(now, now);

        // Delete very old inactive sessions (older than 30 days)
        LocalDateTime cutoff = now.minusDays(30);
        int deleted = sessionRepository.deleteOldInactiveSessions(cutoff);

        if (markedInactive > 0 || deleted > 0) {
            log.info("Session cleanup: {} sessions marked inactive, {} old sessions deleted",
                markedInactive, deleted);
        }
    }

    /**
     * Get session statistics
     */
    public SessionStats getSessionStats() {
        LocalDateTime now = LocalDateTime.now();

        // This is a simple implementation - you could add more complex queries
        List<Session> allActiveSessions = sessionRepository.findAll().stream()
            .filter(s -> s.getIsActive() && s.getRefreshExpiresAt().isAfter(now))
            .toList();

        long totalActive = allActiveSessions.size();
        long uniqueUsers = allActiveSessions.stream()
            .map(s -> s.getUser().getUserId())
            .distinct()
            .count();

        return new SessionStats(totalActive, uniqueUsers);
    }

    // Helper record for session statistics
    public record SessionStats(long totalActiveSessions, long uniqueActiveUsers) {}
}