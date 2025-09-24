package com.nivedha.pathigai.auth.controllers;

import com.nivedha.pathigai.auth.dto.request.RefreshTokenRequest;
import com.nivedha.pathigai.auth.dto.response.RefreshTokenResponse;
import com.nivedha.pathigai.auth.services.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("Received token refresh request");

            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            RefreshTokenResponse response = refreshTokenService.refreshToken(request, ipAddress, userAgent);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    RefreshTokenResponse.builder()
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during token refresh: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    RefreshTokenResponse.builder()
                            .message("Token refresh failed. Please login again.")
                            .build()
            );
        }
    }

    @GetMapping("/token-status")
    public ResponseEntity<?> getTokenStatus(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "No valid token provided"));
            }

            String token = authHeader.substring(7);
            long timeUntilExpiration = refreshTokenService.getTimeUntilExpiration(token);
            boolean nearExpiration = refreshTokenService.isTokenNearExpiration(token, 60); // 1 hour

            return ResponseEntity.ok(Map.of(
                    "timeUntilExpiration", timeUntilExpiration,
                    "nearExpiration", nearExpiration,
                    "expiresAt", System.currentTimeMillis() + timeUntilExpiration
            ));

        } catch (Exception e) {
            log.error("Error checking token status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid token"));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
