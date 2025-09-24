package com.nivedha.pathigai.auth.services;

import com.nivedha.pathigai.auth.dto.request.RefreshTokenRequest;
import com.nivedha.pathigai.auth.dto.response.RefreshTokenResponse;
import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;
    private final SessionService sessionService;

    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        try {
            String refreshToken = request.getRefreshToken();

            // Validate refresh token
            if (!jwtConfig.validateRefreshToken(refreshToken)) {
                throw new IllegalArgumentException("Invalid or expired refresh token");
            }

            // Extract user information from refresh token
            String email = jwtConfig.extractUsername(refreshToken);
            Integer userId = jwtConfig.extractUserId(refreshToken);

            // Get user from database with fresh data
            User user = userRepository.findByEmailAndUserStatus(email, User.UserStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("User not found or inactive"));

            // Verify user ID matches
            if (!user.getUserId().equals(userId)) {
                throw new IllegalArgumentException("Token user ID mismatch");
            }

            // Check if user is enabled
            if (!user.getEnabled()) {
                throw new IllegalArgumentException("User account is disabled");
            }

            // Generate new access token with latest user data
            String newAccessToken = jwtConfig.generateAccessToken(user);

            // Optionally generate new refresh token (token rotation for better security)
            String newRefreshToken = jwtConfig.generateRefreshToken(user.getUserId(), user.getEmail());

            // Update session with new token
            sessionService.updateSession(userId, newAccessToken, ipAddress, userAgent);

            log.info("Token refreshed successfully for user: {} from IP: {}", email, ipAddress);

            return RefreshTokenResponse.builder()
                    .authToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(jwtConfig.getAccessTokenExpiration())
                    .message("Token refreshed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage());
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
