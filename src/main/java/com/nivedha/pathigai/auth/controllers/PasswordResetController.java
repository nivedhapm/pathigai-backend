package com.nivedha.pathigai.auth.controllers;

import com.nivedha.pathigai.auth.dto.request.PasswordResetRequest;
import com.nivedha.pathigai.auth.dto.response.LoginResponse;
import com.nivedha.pathigai.auth.services.PasswordResetService;
import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/password-reset")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final UserRepository userRepository;


    @PostMapping("/initiate")
    public ResponseEntity<LoginResponse> initiateForgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        LoginResponse.builder()
                                .message("Email is required")
                                .build()
                );
            }

            log.info("Received forgot password request for email: {}", email);
            LoginResponse response = passwordResetService.initiateForgotPassword(email.trim());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Forgot password initiation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    LoginResponse.builder()
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during forgot password initiation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    LoginResponse.builder()
                            .message("Password reset initiation failed. Please try again.")
                            .build()
            );
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            log.info("Received password reset completion request for user ID: {}", request.getUserId());

            passwordResetService.resetPassword(request);
            // Fetch and update user verification status
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            user.setEmailVerified(true);
            user.setPhoneVerified(true);
            user.setIsTemporaryPassword(false);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Password reset successful. You can now login with your new password."));

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Password reset completion failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during password reset completion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", "Password reset failed. Please try again.")
            );
        }
    }
}