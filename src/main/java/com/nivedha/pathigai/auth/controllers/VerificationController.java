package com.nivedha.pathigai.auth.controllers;

import com.nivedha.pathigai.auth.dto.request.VerificationRequest;
import com.nivedha.pathigai.auth.dto.request.ChangeVerificationTypeRequest;
import com.nivedha.pathigai.auth.dto.response.VerificationResponse;
import com.nivedha.pathigai.auth.dto.response.ChangeVerificationTypeResponse;
import com.nivedha.pathigai.auth.entities.Verification;
import com.nivedha.pathigai.auth.services.VerificationService;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.auth.entities.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class VerificationController {

    private final VerificationService verificationService;
    private final UserRepository userRepository;


    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verifyOtp(@Valid @RequestBody VerificationRequest request) {
        try {
            log.info("Received OTP verification request for user: {} with type: {} and OTP: {}",
                    request.getUserId(), request.getVerificationType(), request.getOtp());

            // Validate request
            if (request.getUserId() == null) {
                log.warn("User ID is null in verification request");
                return ResponseEntity.badRequest().body(
                        VerificationResponse.builder()
                                .verified(false)
                                .message("User ID is required")
                                .build()
                );
            }

            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                log.warn("OTP is null or empty in verification request");
                return ResponseEntity.badRequest().body(
                        VerificationResponse.builder()
                                .verified(false)
                                .message("OTP is required")
                                .build()
                );
            }

            if (request.getOtp().length() != 6) {
                log.warn("Invalid OTP length: {} for user: {}", request.getOtp().length(), request.getUserId());
                return ResponseEntity.badRequest().body(
                        VerificationResponse.builder()
                                .verified(false)
                                .message("OTP must be 6 digits")
                                .build()
                );
            }

            VerificationResponse response = verificationService.verifyOtp(request);

            if (response.getVerified()) {
                log.info("OTP verification successful for user: {}", request.getUserId());
                // Add this code to update user verification status
                User user = userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

                if (VerificationRequest.VerificationType.SMS.equals(request.getVerificationType())) {
                    user.setPhoneVerified(true);
                } else if (VerificationRequest.VerificationType.EMAIL.equals(request.getVerificationType())) {
                    user.setEmailVerified(true);
                }
                userRepository.save(user);

                return ResponseEntity.ok(response);

            } else {
                log.warn("OTP verification failed for user: {} - {}", request.getUserId(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("OTP verification failed with invalid argument for user: {} - {}",
                    request.getUserId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    VerificationResponse.builder()
                            .verified(false)
                            .message(e.getMessage())
                            .build()
            );
        } catch (IllegalStateException e) {
            log.warn("OTP verification failed with illegal state for user: {} - {}",
                    request.getUserId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    VerificationResponse.builder()
                            .verified(false)
                            .message(e.getMessage())
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Runtime error during OTP verification for user: {} - {}",
                    request.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    VerificationResponse.builder()
                            .verified(false)
                            .message("Verification service temporarily unavailable. Please try again.")
                            .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during OTP verification for user: {} - {}",
                    request.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    VerificationResponse.builder()
                            .verified(false)
                            .message("Verification failed. Please try again.")
                            .build()
            );
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<Map<String, String>> resendVerification(
            @RequestParam Integer userId,
            @RequestParam String verificationType,
            @RequestParam String context) {
        try {
            log.info("Received resend verification request for user: {} with type: {} and context: {}",
                    userId, verificationType, context);

            // Validate parameters
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "User ID is required"));
            }

            if (verificationType == null || verificationType.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Verification type is required"));
            }

            if (context == null || context.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Context is required"));
            }

            Verification.VerificationType type;
            Verification.VerificationContext ctx;

            try {
                type = Verification.VerificationType.valueOf(verificationType.toUpperCase());
                ctx = Verification.VerificationContext.valueOf(context.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid verification type '{}' or context '{}' for user: {}",
                        verificationType, context, userId);
                return ResponseEntity.badRequest().body(
                        Map.of("message", "Invalid verification type or context")
                );
            }

            verificationService.resendVerification(userId, type, ctx);

            log.info("Resend verification successful for user: {}", userId);
            return ResponseEntity.ok(Map.of("message", "Verification code sent successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Resend verification failed with invalid argument for user: {} - {}",
                    userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Resend verification rejected for user: {} - {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Runtime error during resend verification for user: {} - {}",
                    userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", "Email service temporarily unavailable. Please try again.")
            );
        } catch (Exception e) {
            log.error("Unexpected error during resend verification for user: {} - {}",
                    userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", "Failed to resend verification code. Please try again.")
            );
        }
    }

    @PostMapping("/change-type")
    public ResponseEntity<ChangeVerificationTypeResponse> changeVerificationType(
            @Valid @RequestBody ChangeVerificationTypeRequest request) {
        try {
            log.info("Received change verification type request for user: {} to type: {}",
                    request.getUserId(), request.getNewVerificationType());

            // Validate request
            if (request.getUserId() == null) {
                return ResponseEntity.badRequest().body(
                        ChangeVerificationTypeResponse.builder()
                                .message("User ID is required")
                                .build()
                );
            }

            ChangeVerificationTypeResponse response = verificationService.changeVerificationType(request);

            log.info("Change verification type successful for user: {}", request.getUserId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Change verification type failed with invalid argument for user: {} - {}",
                    request.getUserId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    ChangeVerificationTypeResponse.builder()
                            .message(e.getMessage())
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Runtime error during change verification type for user: {} - {}",
                    request.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ChangeVerificationTypeResponse.builder()
                            .message("Service temporarily unavailable. Please try again.")
                            .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during change verification type for user: {} - {}",
                    request.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ChangeVerificationTypeResponse.builder()
                            .message("Failed to change verification type. Please try again.")
                            .build()
            );
        }
    }
}