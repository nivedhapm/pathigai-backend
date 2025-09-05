package com.nivedha.pathigai.auth.services;

import com.nivedha.pathigai.auth.dto.request.PasswordResetRequest;
import com.nivedha.pathigai.auth.dto.response.LoginResponse;
import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.entities.Verification;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.auth.services.utils.MaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final VerificationService verificationService;
    private final PasswordEncoder passwordEncoder;
    private final MaskingUtils maskingUtils;

    public LoginResponse initiateForgotPassword(String email) {
        log.info("Initiating forgot password for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email address"));

        // Check if user account is enabled
        if (!user.getEnabled()) {
            throw new IllegalArgumentException("Account not verified. Please complete account setup first.");
        }

        // Check user status
        if (user.getUserStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is inactive. Please contact support.");
        }

        // Send EMAIL verification by default for password reset (as per your requirement)
        verificationService.sendEmailVerification(user, Verification.VerificationContext.PASSWORD_RESET);

        return LoginResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .maskedEmail(maskingUtils.maskEmail(user.getEmail()))
                .maskedPhone(maskingUtils.maskPhone(user.getPhone()))
                .nextStep("EMAIL_VERIFICATION_REQUIRED")  // Changed from SMS to EMAIL
                .message("Verification code sent to your email. Please verify your identity to reset password.")
                .build();
    }

    public void resetPassword(PasswordResetRequest request) {
        log.info("Resetting password for user: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify that either SMS or EMAIL verification is completed for PASSWORD_RESET context
        boolean smsVerified = verificationService.isVerificationCompleted(
                request.getUserId(),
                Verification.VerificationType.SMS,
                Verification.VerificationContext.PASSWORD_RESET
        );
        boolean emailVerified = verificationService.isVerificationCompleted(
                request.getUserId(),
                Verification.VerificationType.EMAIL,
                Verification.VerificationContext.PASSWORD_RESET
        );

        if (!smsVerified && !emailVerified) {
            throw new IllegalStateException("Verification must be completed before password reset");
        }

        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password confirmation does not match");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setIsTemporaryPassword(false);
        user.setLastPasswordReset(LocalDateTime.now());

        userRepository.save(user);
        log.info("Password reset successfully for user: {}", request.getUserId());
    }
}