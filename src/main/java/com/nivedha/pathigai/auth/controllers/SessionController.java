package com.nivedha.pathigai.auth.controllers;

import com.nivedha.pathigai.auth.entities.Session;
import com.nivedha.pathigai.auth.services.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService sessionService;

    /**
     * Get all active sessions for the current user
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveSessions(@RequestParam Integer userId) {
        try {
            List<Session> sessions = sessionService.getUserActiveSessions(userId);

            // Convert Session entities to response DTOs
            List<Map<String, Object>> sessionDtos = sessions.stream()
                .map(session -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("sessionId", session.getSessionId());
                    dto.put("deviceName", session.getDeviceName());
                    dto.put("ipAddress", session.getIpAddress());
                    dto.put("lastUsedAt", session.getLastUsedAt());
                    dto.put("issuedAt", session.getIssuedAt());
                    dto.put("refreshExpiresAt", session.getRefreshExpiresAt());
                    return dto;
                })
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activeSessions", sessionDtos);
            response.put("sessionCount", sessions.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting active sessions for user {}: {}", userId, e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve active sessions");

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Remove a specific session (logout from specific device)
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> removeSession(
            @PathVariable Integer sessionId,
            @RequestParam Integer userId) {
        try {
            sessionService.removeSession(sessionId, "USER_LOGOUT");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Session removed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error removing session {} for user {}: {}", sessionId, userId, e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to remove session");

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Remove all sessions for a user (logout from all devices)
     */
    @DeleteMapping("/all")
    public ResponseEntity<?> removeAllSessions(@RequestParam Integer userId) {
        try {
            sessionService.removeAllUserSessions(userId, "USER_LOGOUT");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All sessions removed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error removing all sessions for user {}: {}", userId, e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to remove all sessions");

            return ResponseEntity.internalServerError().body(response);
        }
    }
}