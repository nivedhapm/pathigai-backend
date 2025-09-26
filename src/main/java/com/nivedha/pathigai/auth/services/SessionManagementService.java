package com.nivedha.pathigai.auth.services;

import com.nivedha.pathigai.auth.entities.Session;
import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.repositories.SessionRepository;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManagementService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    @Value("${app.session.max-concurrent-sessions:3}")
    private int maxConcurrentSessions;

    @Value("${app.session.device-fingerprint-enabled:true}")
    private boolean deviceFingerprintEnabled;

    @Value("${app.session.strict-device-binding:true}")
    private boolean strictDeviceBinding;

    /**
     * Creates a device fingerprint from IP address, User-Agent, and optional additional entropy
     */
    public String generateDeviceFingerprint(String ipAddress, String userAgent, String additionalEntropy) {
        if (!deviceFingerprintEnabled) {
            return "default-device";
        }

        String combined = String.format("%s|%s|%s",
            ipAddress != null ? ipAddress : "unknown",
            userAgent != null ? userAgent : "unknown",
            additionalEntropy != null ? additionalEntropy : "");

        return DigestUtils.md5DigestAsHex(combined.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a friendly device name from User-Agent
     */
    public String generateDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown Device";
        }

        String deviceName = "Unknown Device";

        // Extract browser information
        if (userAgent.contains("Chrome")) {
            deviceName = "Chrome";
        } else if (userAgent.contains("Firefox")) {
            deviceName = "Firefox";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            deviceName = "Safari";
        } else if (userAgent.contains("Edge")) {
            deviceName = "Edge";
        }

        // Extract OS information
        if (userAgent.contains("Windows")) {
            deviceName += " on Windows";
        } else if (userAgent.contains("Mac OS")) {
            deviceName += " on Mac";
        } else if (userAgent.contains("Linux")) {
            deviceName += " on Linux";
        } else if (userAgent.contains("Android")) {
            deviceName += " on Android";
        } else if (userAgent.contains("iOS")) {
            deviceName += " on iOS";
        }

        return deviceName;
    }

    /**
     * Hashes a token for secure storage
     */
    public String hashToken(String token) {
        return DigestUtils.md5DigestAsHex(token.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates or reuses a session for the given user and device - DIRECT JPA VERSION (bypassing stored procedure)
     */
    public SessionResult createOrReuseSession(SessionRequest request) {
        try {
            String deviceFingerprint = generateDeviceFingerprint(
                request.getIpAddress(),
                request.getUserAgent(),
                null
            );

            String deviceName = generateDeviceName(request.getUserAgent());

            log.info("Creating/reusing session for user {} with device fingerprint: {}",
                request.getUserId(), deviceFingerprint);

            // First, deactivate any existing active sessions for this user-device combination
            // This prevents the duplicate key issue while preserving audit history
            int deactivatedSessions = sessionRepository.deactivateExistingUserDeviceSessions(
                request.getUserId(), deviceFingerprint, LocalDateTime.now(), Session.RevokeReason.NEW_LOGIN);

            if (deactivatedSessions > 0) {
                log.info("Deactivated {} existing sessions for user {} on device {} due to new login",
                    deactivatedSessions, request.getUserId(), deviceFingerprint.substring(0, 8));
            }

            // Check if user has reached max session limit across all devices
            Long activeSessionCount = sessionRepository.countActiveSessionsByUserId(
                request.getUserId(), LocalDateTime.now());

            if (activeSessionCount >= maxConcurrentSessions) {
                // Revoke oldest session across all devices
                List<Session> oldestSessions = sessionRepository.findOldestActiveSessionsByUserId(
                    request.getUserId(), LocalDateTime.now());

                if (!oldestSessions.isEmpty()) {
                    Session oldestSession = oldestSessions.get(0);
                    sessionRepository.deactivateSessionById(oldestSession.getSessionId(),
                        LocalDateTime.now(), Session.RevokeReason.MAX_SESSIONS_EXCEEDED);

                    log.info("Revoked oldest session {} for user {} due to max session limit",
                        oldestSession.getSessionId(), request.getUserId());
                }
            }

            // Always create new session (no more unique constraint blocking us)
            User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));

            Session newSession = Session.builder()
                .user(user)
                .deviceFingerprint(deviceFingerprint)
                .deviceName(deviceName)
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .accessTokenHash(hashToken(request.getAccessToken()))
                .refreshTokenHash(hashToken(request.getRefreshToken()))
                .refreshTokenVersion(1)
                .accessExpiresAt(request.getAccessExpiresAt())
                .refreshExpiresAt(request.getRefreshExpiresAt())
                .lastUsedAt(LocalDateTime.now())
                .isActive(true)
                .build();

            Session savedSession = sessionRepository.save(newSession);
            log.info("Session CREATED for user {} with ID: {}", request.getUserId(), savedSession.getSessionId());

            return SessionResult.builder()
                .sessionId(savedSession.getSessionId())
                .action("CREATED")
                .deviceFingerprint(deviceFingerprint)
                .deviceName(deviceName)
                .success(true)
                .build();

        } catch (Exception e) {
            log.error("Error creating session for user {}: {}", request.getUserId(), e.getMessage(), e);
            return SessionResult.builder()
                .success(false)
                .errorMessage("Internal server error during session creation: " + e.getMessage())
                .build();
        }
    }

    /**
     * Refreshes tokens using DIRECT JPA calls (bypassing stored procedure)
     */
    public RefreshTokenResult refreshToken(String refreshToken, String newAccessToken,
                                         String newRefreshToken, LocalDateTime newAccessExpiresAt,
                                         LocalDateTime newRefreshExpiresAt, String ipAddress) {
        try {
            String oldRefreshTokenHash = hashToken(refreshToken);

            log.info("🔄 REFRESH TOKEN: Looking for session with refresh token hash...");

            // Find session by refresh token hash
            Optional<Session> sessionOpt = sessionRepository.findByRefreshTokenHashAndIsActiveTrueAndRefreshExpiresAtAfter(
                oldRefreshTokenHash, LocalDateTime.now());

            if (sessionOpt.isEmpty()) {
                log.warn("❌ REFRESH TOKEN: No active session found for provided refresh token");
                return RefreshTokenResult.builder()
                    .success(false)
                    .errorMessage("Invalid or expired refresh token")
                    .build();
            }

            Session session = sessionOpt.get();

            log.info("✅ REFRESH TOKEN: Found session {} for user {}", session.getSessionId(), session.getUser().getUserId());
            log.info("🔄 REFRESH TOKEN: Updating session with new tokens...");

            // Update session with new tokens
            session.setAccessTokenHash(hashToken(newAccessToken));
            session.setRefreshTokenHash(hashToken(newRefreshToken));
            session.setRefreshTokenVersion(session.getRefreshTokenVersion() + 1);
            session.setAccessExpiresAt(newAccessExpiresAt);
            session.setRefreshExpiresAt(newRefreshExpiresAt);
            session.setLastUsedAt(LocalDateTime.now());

            Session updatedSession = sessionRepository.save(session);

            log.info("✅ REFRESH TOKEN: Session updated successfully!");
            log.info("   Session ID: {}", updatedSession.getSessionId());
            log.info("   User ID: {}", updatedSession.getUser().getUserId());
            log.info("   New Token Version: {}", updatedSession.getRefreshTokenVersion());
            log.info("   New Access Expires: {}", updatedSession.getAccessExpiresAt());
            log.info("   New Refresh Expires: {}", updatedSession.getRefreshExpiresAt());

            return RefreshTokenResult.builder()
                .sessionId(updatedSession.getSessionId())
                .userId(updatedSession.getUser().getUserId())
                .success(true)
                .build();

        } catch (Exception e) {
            log.error("❌ REFRESH TOKEN ERROR: {}", e.getMessage(), e);
            return RefreshTokenResult.builder()
                .success(false)
                .errorMessage("Internal server error during token refresh: " + e.getMessage())
                .build();
        }
    }

    /**
     * Revokes all sessions for a user (security breach response)
     */
    public RevokeSessionsResult revokeAllUserSessions(Integer userId, String reason) {
        try {
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("RevokeAllUserSessions")
                .declareParameters(
                    new SqlParameter("p_user_id", Types.INTEGER),
                    new SqlParameter("p_reason", Types.VARCHAR),
                    new SqlOutParameter("p_revoked_count", Types.INTEGER),
                    new SqlOutParameter("p_success", Types.BOOLEAN),
                    new SqlOutParameter("p_error_message", Types.VARCHAR)
                );

            Map<String, Object> inParams = new HashMap<>();
            inParams.put("p_user_id", userId);
            inParams.put("p_reason", reason);

            Map<String, Object> result = jdbcCall.execute(inParams);

            Boolean success = (Boolean) result.get("p_success");
            if (success != null && success) {
                Integer revokedCount = (Integer) result.get("p_revoked_count");
                log.info("Revoked {} sessions for user {} due to: {}", revokedCount, userId, reason);
                return RevokeSessionsResult.builder()
                    .revokedCount(revokedCount)
                    .success(true)
                    .build();
            } else {
                String errorMessage = (String) result.get("p_error_message");
                log.error("Failed to revoke sessions for user {}: {}", userId, errorMessage);
                return RevokeSessionsResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
            }
        } catch (Exception e) {
            log.error("Error revoking sessions for user {}: {}", userId, e.getMessage(), e);
            return RevokeSessionsResult.builder()
                .success(false)
                .errorMessage("Internal server error during session revocation")
                .build();
        }
    }

    /**
     * Revokes a specific session
     */
    public boolean revokeSession(Integer sessionId, String reason) {
        try {
            int updated = jdbcTemplate.update(
                "UPDATE sessions SET is_active = FALSE, revoked_at = NOW(), revoke_reason = ? WHERE session_id = ? AND is_active = TRUE",
                reason, sessionId
            );

            if (updated > 0) {
                log.info("Revoked session {} due to: {}", sessionId, reason);
                return true;
            } else {
                log.warn("Session {} not found or already revoked", sessionId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error revoking session {}: {}", sessionId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates if a refresh token exists and is valid
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        try {
            String hashedToken = hashToken(refreshToken);
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE refresh_token_hash = ? AND is_active = TRUE AND refresh_expires_at > NOW()",
                Integer.class, hashedToken
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Error validating refresh token: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gets active session count for a user
     */
    public int getActiveSessionCount(Integer userId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE user_id = ? AND is_active = TRUE",
                Integer.class, userId
            );
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Error getting active session count for user {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    public static class SessionRequest {
        private Integer userId;
        private String accessToken;
        private String refreshToken;
        private LocalDateTime accessExpiresAt;
        private LocalDateTime refreshExpiresAt;
        private String ipAddress;
        private String userAgent;
    }

    @lombok.Data
    @lombok.Builder
    public static class SessionResult {
        private Integer sessionId;
        private String action; // "CREATED" or "REUSED"
        private String deviceFingerprint;
        private String deviceName;
        private boolean success;
        private String errorMessage;
    }

    @lombok.Data
    @lombok.Builder
    public static class RefreshTokenResult {
        private Integer sessionId;
        private Integer userId;
        private boolean success;
        private String errorMessage;
    }

    @lombok.Data
    @lombok.Builder
    public static class RevokeSessionsResult {
        private Integer revokedCount;
        private boolean success;
        private String errorMessage;
    }
}
