package com.nivedha.pathigai.auth.controllers;

import com.nivedha.pathigai.auth.dto.request.LoginRequest;
import com.nivedha.pathigai.auth.dto.request.PasswordResetRequest;
import com.nivedha.pathigai.auth.dto.response.LoginResponse;
import com.nivedha.pathigai.auth.services.LoginService;
import com.nivedha.pathigai.auth.services.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/login")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class LoginController {

    private final LoginService loginService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/authenticate")
    public ResponseEntity<LoginResponse> authenticateUser(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("Received login authentication request for email: {}", request.getEmail());

            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            LoginResponse response = loginService.authenticateUser(request, ipAddress, userAgent);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Login authentication failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    LoginResponse.builder()
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during login authentication: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    LoginResponse.builder()
                            .message("Login failed. Please try again.")
                            .build()
            );
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<LoginResponse> completeLogin(
            @RequestParam Integer userId,
            HttpServletRequest httpRequest) {
        try {
            log.info("Received login completion request for user ID: {}", userId);

            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            LoginResponse response = loginService.completeLogin(userId, ipAddress, userAgent);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Login completion failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    LoginResponse.builder()
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during login completion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    LoginResponse.builder()
                            .message("Login completion failed. Please try again.")
                            .build()
            );
        }
    }

    @PostMapping("/reset-temporary-password")
    public ResponseEntity<Map<String, String>> resetTemporaryPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            log.info("Received temporary password reset request for user ID: {}", request.getUserId());

            loginService.resetTemporaryPassword(request);
            return ResponseEntity.ok(Map.of("message", "Password reset successful. Please login with your new password."));

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Temporary password reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during temporary password reset: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", "Password reset failed. Please try again.")
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        try {
            log.info("Received logout request");

            String authHeader = request.getHeader("Authorization");
            String jwtToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7);
            }

            loginService.logout(jwtToken);
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));

        } catch (Exception e) {
            log.error("Unexpected error during logout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", "Logout failed. Please try again.")
            );
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