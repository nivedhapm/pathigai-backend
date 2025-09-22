package com.nivedha.pathigai.auth.services;

import com.nivedha.pathigai.auth.entities.Session;
import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.entities.Verification;
import com.nivedha.pathigai.auth.repositories.SessionRepository;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.config.JwtConfig;
import com.nivedha.pathigai.auth.dto.request.LoginRequest;
import com.nivedha.pathigai.auth.dto.request.PasswordResetRequest;
import com.nivedha.pathigai.auth.dto.response.LoginResponse;
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
public class LoginService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final VerificationService verificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final MaskingUtils maskingUtils;

    public LoginResponse authenticateUser(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Starting authentication for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // Check if user account is enabled
        if (!user.getEnabled()) {
            throw new IllegalArgumentException("Account not verified. Please complete account setup first.");
        }

        // Check user status
        if (user.getUserStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is inactive. Please contact support.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("User authenticated successfully: {}", user.getUserId());

        // Send EMAIL verification by default (as per your requirement)
        verificationService.sendEmailVerification(user, Verification.VerificationContext.LOGIN);

        return LoginResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .maskedEmail(maskingUtils.maskEmail(user.getEmail()))
                .maskedPhone(maskingUtils.maskPhone(user.getPhone()))
                .isTemporaryPassword(user.getIsTemporaryPassword())
                .nextStep("EMAIL_VERIFICATION_REQUIRED")  // Changed from SMS to EMAIL
                .message("Authentication successful. Please verify your identity.")
                .build();
    }

    public LoginResponse completeLogin(Integer userId, String ipAddress, String userAgent) {
        log.info("Completing login for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify that either SMS or EMAIL verification is completed for LOGIN context
        boolean smsVerified = verificationService.isVerificationCompleted(
                userId, Verification.VerificationType.SMS, Verification.VerificationContext.LOGIN
        );
        boolean emailVerified = verificationService.isVerificationCompleted(
                userId, Verification.VerificationType.EMAIL, Verification.VerificationContext.LOGIN
        );

        if (!smsVerified && !emailVerified) {
            throw new IllegalStateException("Verification must be completed before login");
        }

        // If user has temporary password, return special response
        if (user.getIsTemporaryPassword()) {
            return LoginResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .isTemporaryPassword(true)
                    .nextStep("PASSWORD_RESET_REQUIRED")
                    .message("Please reset your temporary password")
                    .build();
        }

        // Generate JWT tokens with profile information
        String accessToken = jwtConfig.generateAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoleName(),
                user.getProfileName(),
                user.getCompanyId()
        );
        String refreshToken = jwtConfig.generateRefreshToken(user.getUserId(), user.getEmail());

        // Create session record
        Session session = Session.builder()
                .user(user)
                .jwtToken(accessToken)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
// Expires in minutes
                .expiresAt(LocalDateTime.now().plusMinutes(jwtConfig.getAccessTokenExpiration()))
                
                .isActive(true)
                .build();

        sessionRepository.save(session);
        log.info("Session created for user: {}", userId);

        return LoginResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .jwtToken(accessToken)
                .refreshToken(refreshToken)
                .tokenExpiresIn(jwtConfig.getAccessTokenExpiration())
                .nextStep("LOGIN_SUCCESS")
                .message("Login successful")
                .build();
    }

    public void resetTemporaryPassword(PasswordResetRequest request) {
        log.info("Resetting temporary password for user: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify user has completed verification
        boolean verified = verificationService.isVerificationCompleted(
                request.getUserId(),
                Verification.VerificationType.SMS,
                Verification.VerificationContext.LOGIN
        ) || verificationService.isVerificationCompleted(
                request.getUserId(),
                Verification.VerificationType.EMAIL,
                Verification.VerificationContext.LOGIN
        );

        if (!verified) {
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
        log.info("Temporary password reset successfully for user: {}", request.getUserId());
    }

    public void logout(String jwtToken) {
        log.info("Processing logout request");

        if (jwtToken != null) {
            // Deactivate session
            sessionRepository.deactivateSessionByToken(jwtToken);
            log.info("Session deactivated for token");
        }
    }

    public void logoutAllSessions(Integer userId) {
        log.info("Logging out all sessions for user: {}", userId);
        sessionRepository.deactivateAllUserSessions(userId);
    }
}