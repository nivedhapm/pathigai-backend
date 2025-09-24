package com.nivedha.pathigai.auth.services;

import com.nivedha.pathigai.auth.dto.request.RefreshTokenRequest;
import com.nivedha.pathigai.auth.dto.response.RefreshTokenResponse;
import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.entities.Session;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.auth.repositories.SessionRepository;
import com.nivedha.pathigai.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final SessionService sessionService;

    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        try {
            String refreshToken = request.getRefreshToken();
            log.info("🔄 REFRESH TOKEN REQUEST RECEIVED");
            log.info("   📍 IP Address: {}", ipAddress);
            log.info("   🖥️ User Agent: {}", userAgent);
            log.info("   🔑 Refresh Token (first 20 chars): {}...", refreshToken.substring(0, Math.min(20, refreshToken.length())));

            // Validate refresh token
            log.info("   ✅ Validating refresh token...");
            if (!jwtConfig.validateRefreshToken(refreshToken)) {
                log.error("   ❌ REFRESH TOKEN VALIDATION FAILED - Invalid or expired");
                throw new IllegalArgumentException("Invalid or expired refresh token");
            }
            log.info("   ✅ Refresh token validation PASSED");

            // Extract user information from refresh token
            String email = jwtConfig.extractUsername(refreshToken);
            Integer userId = jwtConfig.extractUserId(refreshToken);
            log.info("   👤 Extracted from token - User ID: {}, Email: {}", userId, email);

            // Get user from database with fresh data
            User user = userRepository.findByEmailAndUserStatus(email, User.UserStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("User not found or inactive"));
            log.info("   📂 User found in database - ID: {}, Email: {}, Status: {}", user.getUserId(), user.getEmail(), user.getUserStatus());

            // Verify user ID matches
            if (!user.getUserId().equals(userId)) {
                log.error("   ❌ USER ID MISMATCH - Token: {}, Database: {}", userId, user.getUserId());
                throw new IllegalArgumentException("Token user ID mismatch");
            }

            // Check if user is enabled
            if (!user.getEnabled()) {
                log.error("   ❌ USER ACCOUNT DISABLED - User ID: {}", userId);
                throw new IllegalArgumentException("User account is disabled");
            }

            // Generate new access token with latest user data
            log.info("   🔄 Generating new access token...");
            String newAccessToken = jwtConfig.generateAccessToken(user);
            log.info("   ✅ New access token generated (first 20 chars): {}...", newAccessToken.substring(0, Math.min(20, newAccessToken.length())));

            // Generate new refresh token (for security token rotation)
            log.info("   🔄 Generating new refresh token...");
            String newRefreshToken = jwtConfig.generateRefreshToken(user.getUserId(), user.getEmail());
            log.info("   ✅ New refresh token generated (first 20 chars): {}...", newRefreshToken.substring(0, Math.min(20, newRefreshToken.length())));

            // ✅ FIX: Get existing session to preserve original refresh expiry
            log.info("   📂 Looking up existing session to preserve original expiry...");
            Optional<Session> existingSessionOpt = sessionRepository.findByRefreshTokenHashAndIsActiveTrueAndRefreshExpiresAtAfter(
                DigestUtils.md5DigestAsHex(refreshToken.getBytes()), LocalDateTime.now());

            if (existingSessionOpt.isEmpty()) {
                log.error("   ❌ EXISTING SESSION NOT FOUND for refresh token");
                throw new IllegalArgumentException("Session not found for refresh token");
            }

            Session existingSession = existingSessionOpt.get();
            LocalDateTime originalRefreshExpiresAt = existingSession.getRefreshExpiresAt();

            // Check if original refresh token has expired (session should end)
            if (LocalDateTime.now().isAfter(originalRefreshExpiresAt)) {
                log.warn("   ⏰ ORIGINAL REFRESH TOKEN EXPIRED - Session should end");
                throw new IllegalArgumentException("Session has expired. Please login again.");
            }

            // Calculate new access token expiry (only extend access token)
            LocalDateTime newAccessExpiresAt = LocalDateTime.now().plusSeconds(jwtConfig.getAccessTokenExpiration() / 1000);

            // ✅ KEY FIX: Preserve original refresh token expiry (don't extend it!)
            LocalDateTime newRefreshExpiresAt = originalRefreshExpiresAt;

            log.info("   🔄 Updating session with new tokens...");
            log.info("   ⏰ New access token expires at: {}", newAccessExpiresAt);
            log.info("   ⏰ Refresh token expires at: {} (PRESERVED from original session)", newRefreshExpiresAt);
            log.info("   ⏳ Session will end in: {} minutes",
                java.time.Duration.between(LocalDateTime.now(), newRefreshExpiresAt).toMinutes());

            sessionService.refreshSession(refreshToken, newAccessToken, newRefreshToken,
                newAccessExpiresAt, newRefreshExpiresAt, ipAddress);

            log.info("   ✅ SESSION UPDATED SUCCESSFULLY");
            log.info("🎉 TOKEN REFRESH COMPLETED SUCCESSFULLY for user: {} from IP: {}", email, ipAddress);

            return RefreshTokenResponse.builder()
                    .authToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(jwtConfig.getAccessTokenExpiration())
                    .message("Token refreshed successfully")
                    .build();

        } catch (Exception e) {
            log.error("❌ REFRESH TOKEN FAILED for IP: {} - Error: {}", ipAddress, e.getMessage(), e);
            throw new IllegalArgumentException("Failed to refresh token: " + e.getMessage());
        }
    }

    public boolean isTokenNearExpiration(String token, long minutesBeforeExpiration) {
        try {
            long timeUntilExpiration = jwtConfig.getTimeUntilExpiration(token);
            long threshold = minutesBeforeExpiration * 60 * 1000; // Convert minutes to milliseconds
            return timeUntilExpiration <= threshold && timeUntilExpiration > 0;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true; // Assume near expiration if we can't check
        }
    }

    public long getTimeUntilExpiration(String token) {
        return jwtConfig.getTimeUntilExpiration(token);
    }
}
