package com.nivedha.pathigai.auth.controllers;

import com.nivedha.pathigai.auth.dto.response.AuthResponse;
import com.nivedha.pathigai.auth.dto.response.SessionResponse;
import com.nivedha.pathigai.auth.services.EnhancedAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class EnhancedAuthController {

    private final EnhancedAuthService enhancedAuthService;

    @GetMapping("/session-status")
    public ResponseEntity<SessionResponse> getSessionStatus(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        SessionResponse response = enhancedAuthService.validateSession(token);

        if (response.getValid()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/extend-session")
    public ResponseEntity<Void> extendSession(Authentication authentication) {
        enhancedAuthService.extendSession(authentication.getName());
        return ResponseEntity.ok().build();
    }
}
