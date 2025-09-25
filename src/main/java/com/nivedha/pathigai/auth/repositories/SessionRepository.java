package com.nivedha.pathigai.auth.repositories;

import com.nivedha.pathigai.auth.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {

    // Find session by refresh token hash
    Optional<Session> findByRefreshTokenHashAndIsActiveTrueAndRefreshExpiresAtAfter(
        String refreshTokenHash, LocalDateTime currentTime);

    // Find session by access token hash
    Optional<Session> findByAccessTokenHashAndIsActiveTrueAndAccessExpiresAtAfter(
        String accessTokenHash, LocalDateTime currentTime);

    // Find existing active session for user and device
    Optional<Session> findByUser_UserIdAndDeviceFingerprintAndIsActiveTrue(
        Integer userId, String deviceFingerprint);

    // Find all active sessions for a user
    @Query("SELECT s FROM Session s WHERE s.user.userId = :userId AND s.isActive = true AND s.refreshExpiresAt > :currentTime")
    List<Session> findActiveSessionsByUserId(@Param("userId") Integer userId, @Param("currentTime") LocalDateTime currentTime);

    // Count active sessions for a user
    @Query("SELECT COUNT(s) FROM Session s WHERE s.user.userId = :userId AND s.isActive = true AND s.refreshExpiresAt > :currentTime")
    Long countActiveSessionsByUserId(@Param("userId") Integer userId, @Param("currentTime") LocalDateTime currentTime);

    // Find oldest active session for a user (for max session limit enforcement)
    @Query("SELECT s FROM Session s WHERE s.user.userId = :userId AND s.isActive = true AND s.refreshExpiresAt > :currentTime ORDER BY s.lastUsedAt ASC")
    List<Session> findOldestActiveSessionsByUserId(@Param("userId") Integer userId, @Param("currentTime") LocalDateTime currentTime);

    // Deactivate all sessions for a user
    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.isActive = false, s.revokedAt = :revokedAt, s.revokeReason = :reason WHERE s.user.userId = :userId AND s.isActive = true")
    int deactivateAllUserSessions(@Param("userId") Integer userId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") Session.RevokeReason reason);

    // Deactivate session by refresh token
    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.isActive = false, s.revokedAt = :revokedAt, s.revokeReason = :reason WHERE s.refreshTokenHash = :refreshTokenHash")
    int deactivateSessionByRefreshToken(@Param("refreshTokenHash") String refreshTokenHash, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") Session.RevokeReason reason);

    // Deactivate session by access token (for logout)
    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.isActive = false, s.revokedAt = :revokedAt, s.revokeReason = :reason WHERE s.accessTokenHash = :accessTokenHash")
    int deactivateSessionByAccessToken(@Param("accessTokenHash") String accessTokenHash, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") Session.RevokeReason reason);

    // Deactivate session by ID
    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.isActive = false, s.revokedAt = :revokedAt, s.revokeReason = :reason WHERE s.sessionId = :sessionId")
    int deactivateSessionById(@Param("sessionId") Integer sessionId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") Session.RevokeReason reason);

    // Update session tokens and timestamps
    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.accessTokenHash = :accessTokenHash, s.refreshTokenHash = :refreshTokenHash, " +
           "s.refreshTokenVersion = s.refreshTokenVersion + 1, s.accessExpiresAt = :accessExpiresAt, " +
           "s.refreshExpiresAt = :refreshExpiresAt, s.lastUsedAt = :lastUsedAt " +
           "WHERE s.sessionId = :sessionId")
    int updateSessionTokens(@Param("sessionId") Integer sessionId,
                           @Param("accessTokenHash") String accessTokenHash,
                           @Param("refreshTokenHash") String refreshTokenHash,
                           @Param("accessExpiresAt") LocalDateTime accessExpiresAt,
                           @Param("refreshExpiresAt") LocalDateTime refreshExpiresAt,
                           @Param("lastUsedAt") LocalDateTime lastUsedAt);

    // Update last used timestamp
    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.lastUsedAt = :lastUsedAt WHERE s.sessionId = :sessionId")
    int updateLastUsedAt(@Param("sessionId") Integer sessionId, @Param("lastUsedAt") LocalDateTime lastUsedAt);

    // Cleanup expired sessions (hard delete old inactive sessions)
    @Modifying
    @Transactional
    @Query("DELETE FROM Session s WHERE s.isActive = false AND s.revokedAt < :cutoffDate")
    int deleteOldInactiveSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Deactivate expired sessions (mark as inactive)
    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.isActive = false, s.revokedAt = :currentTime, s.revokeReason = 'EXPIRED' " +
           "WHERE s.isActive = true AND s.refreshExpiresAt < :currentTime")
    int deactivateExpiredSessions(@Param("currentTime") LocalDateTime currentTime);

    // Find sessions expiring within a certain time frame (for notifications)
    @Query("SELECT s FROM Session s WHERE s.isActive = true AND s.refreshExpiresAt BETWEEN :currentTime AND :warningTime")
    List<Session> findSessionsExpiringWithinTimeFrame(@Param("currentTime") LocalDateTime currentTime,
                                                     @Param("warningTime") LocalDateTime warningTime);

    // Mark expired sessions as inactive
    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.isActive = false, s.revokedAt = :revokedAt, s.revokeReason = 'TOKEN_REFRESH' " +
           "WHERE s.isActive = true AND s.refreshExpiresAt < :currentTime")
    int markExpiredSessionsAsInactive(@Param("currentTime") LocalDateTime currentTime, @Param("revokedAt") LocalDateTime revokedAt);

    // Find sessions by device fingerprint pattern (for security analysis)
    @Query("SELECT s FROM Session s WHERE s.deviceFingerprint = :deviceFingerprint AND s.isActive = true")
    List<Session> findActiveSessionsByDeviceFingerprint(@Param("deviceFingerprint") String deviceFingerprint);

    // Find sessions by IP address (for security analysis)
    @Query("SELECT s FROM Session s WHERE s.ipAddress = :ipAddress AND s.isActive = true AND s.refreshExpiresAt > :currentTime")
    List<Session> findActiveSessionsByIpAddress(@Param("ipAddress") String ipAddress, @Param("currentTime") LocalDateTime currentTime);

    // Check if refresh token exists and is valid
    @Query("SELECT COUNT(s) > 0 FROM Session s WHERE s.refreshTokenHash = :refreshTokenHash AND s.isActive = true AND s.refreshExpiresAt > :currentTime")
    boolean existsByRefreshTokenHashAndActive(@Param("refreshTokenHash") String refreshTokenHash, @Param("currentTime") LocalDateTime currentTime);
}