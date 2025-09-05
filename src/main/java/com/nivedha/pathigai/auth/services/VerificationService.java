package com.nivedha.pathigai.auth.services;

import com.nivedha.pathigai.auth.dto.request.VerificationRequest;
import com.nivedha.pathigai.auth.dto.request.ChangeVerificationTypeRequest;
import com.nivedha.pathigai.auth.dto.response.VerificationResponse;
import com.nivedha.pathigai.auth.dto.response.ChangeVerificationTypeResponse;
import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.entities.Verification;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.auth.repositories.VerificationRepository;
import com.nivedha.pathigai.auth.services.external.SmsService;
import com.nivedha.pathigai.auth.services.external.EmailService;
import com.nivedha.pathigai.auth.services.utils.MaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final MaskingUtils maskingUtils;

    @Value("${app.verification.otp-length:6}")
    private int otpLength;

    @Value("${app.verification.sms-expiry-minutes:10}")
    private int smsExpiryMinutes;

    @Value("${app.verification.email-expiry-minutes:15}")
    private int emailExpiryMinutes;

    @Value("${app.verification.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.verification.max-resends:3}")
    private int maxResends;

    @Value("${app.verification.sms-dev-otp:123456}")
    private String smsDevOtp;

    @Value("${app.verification.email-use-real-otp:true}")
    private boolean emailUseRealOtp;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void sendSmsVerification(User user, Verification.VerificationContext context) {
        log.info("Sending SMS verification to user: {}", user.getUserId());

        try {
            // SMS always uses hardcoded OTP
            String otp = smsDevOtp;
            String otpHash = passwordEncoder.encode(otp);

            log.info("SMS OTP for user {} is: {} (hardcoded)", user.getUserId(), otp);

            // Invalidate existing SMS verifications for this context
            invalidateExistingVerifications(user.getUserId(), Verification.VerificationType.SMS, context);

            // Create verification record
            Verification verification = Verification.builder()
                    .user(user)
                    .verificationType(Verification.VerificationType.SMS)
                    .context(context)
                    .otpHash(otpHash)
                    .expiresAt(LocalDateTime.now().plusMinutes(smsExpiryMinutes))
                    .verified(false)
                    .attemptCount(0)
                    .resendCount(0)
                    .build();

            verificationRepository.save(verification);

            String message = String.format("Your Pathigai verification code is: %s. Valid for %d minutes. Do not share this code with anyone.",
                    otp, smsExpiryMinutes);

            log.info("SMS message for user {}: {}", user.getUserId(), message);
            log.info("SMS verification prepared for user: {}", user.getUserId());

        } catch (Exception e) {
            log.error("Failed to send SMS verification for user: {} - {}", user.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS verification: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void sendEmailVerification(User user, Verification.VerificationContext context) {
        log.info("Sending email verification to user: {} with email: {}", user.getUserId(), user.getEmail());

        try {
            // For SIGNUP context, check if SMS verification is completed first
            if (context == Verification.VerificationContext.SIGNUP) {
                boolean smsVerified = isVerificationCompleted(
                        user.getUserId(),
                        Verification.VerificationType.SMS,
                        Verification.VerificationContext.SIGNUP
                );

                if (!smsVerified) {
                    log.warn("SMS verification not completed for user: {} - cannot proceed with email verification", user.getUserId());
                    throw new IllegalStateException("SMS verification must be completed before email verification");
                }

                log.info("SMS verification confirmed for user: {}, proceeding with email verification", user.getUserId());
            }

            // Invalidate existing email verifications FIRST to prevent duplicates
            invalidateExistingVerifications(user.getUserId(), Verification.VerificationType.EMAIL, context);

            // Email always generates real random OTP
            String otp = generateOtp();
            String otpHash = passwordEncoder.encode(otp);

            log.info("Generated email OTP for user {}: {} (will be sent to: {})", user.getUserId(), otp, user.getEmail());

            // Create verification record
            Verification verification = Verification.builder()
                    .user(user)
                    .verificationType(Verification.VerificationType.EMAIL)
                    .context(context)
                    .otpHash(otpHash)
                    .expiresAt(LocalDateTime.now().plusMinutes(emailExpiryMinutes))
                    .verified(false)
                    .attemptCount(0)
                    .resendCount(0)
                    .lastResend(LocalDateTime.now())
                    .build();

            // Save verification BEFORE sending email
            Verification savedVerification = verificationRepository.save(verification);
            log.info("Saved email verification record with ID: {} for user: {}", savedVerification.getVerificationId(), user.getUserId());

            // Send Email with real OTP
            String subject = "Pathigai Email Verification";
            String emailBody = String.format(
                    "Dear %s,\n\nYour Pathigai email verification code is: %s\n\nThis code is valid for %d minutes. Do not share this code with anyone.\n\nBest regards,\nPathigai Team",
                    user.getFullName() != null ? user.getFullName() : "User",
                    otp,
                    emailExpiryMinutes);

            emailService.sendEmail(user.getEmail(), subject, emailBody);
            log.info("Email verification sent successfully for user: {} to email: {} with OTP: {}",
                    user.getUserId(), user.getEmail(), otp);

        } catch (IllegalStateException e) {
            // Re-throw business logic exceptions
            throw e;
        } catch (Exception e) {
            log.error("Failed to send email verification for user: {} to email: {}: {}",
                    user.getUserId(), user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    @Transactional
    private void invalidateExistingVerifications(Integer userId, Verification.VerificationType type,
                                                 Verification.VerificationContext context) {
        log.info("Invalidating existing {} verifications for user: {} and context: {}", type, userId, context);

        List<Verification> existingVerifications = verificationRepository.findExistingActiveVerifications(
                userId, type, context, LocalDateTime.now()
        );

        if (!existingVerifications.isEmpty()) {
            log.info("Found {} existing active {} verifications to invalidate for user: {}",
                    existingVerifications.size(), type, userId);

            for (Verification existing : existingVerifications) {
                existing.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                verificationRepository.save(existing);
                log.info("Invalidated {} verification ID: {} for user: {}", type, existing.getVerificationId(), userId);
            }
        } else {
            log.info("No existing active {} verifications found for user: {}", type, userId);
        }
    }

    @Transactional
    public VerificationResponse verifyOtp(VerificationRequest request) {
        log.info("Verifying OTP for user: {} with type: {} and OTP: {}",
                request.getUserId(), request.getVerificationType(), request.getOtp());

        try {
            // Validate user exists
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + request.getUserId()));

            // Map request types to entity types
            Verification.VerificationType verificationType;
            Verification.VerificationContext verificationContext;

            try {
                verificationType = mapVerificationType(request.getVerificationType());
                verificationContext = mapVerificationContext(request.getContext());
            } catch (IllegalArgumentException e) {
                log.error("Invalid verification type or context: {}", e.getMessage());
                return VerificationResponse.builder()
                        .verified(false)
                        .message("Invalid verification type or context")
                        .build();
            }

            // Find active verification
            Optional<Verification> verificationOpt = verificationRepository.findActiveVerification(
                    request.getUserId(),
                    verificationType,
                    verificationContext,
                    LocalDateTime.now()
            );

            if (verificationOpt.isEmpty()) {
                log.warn("No active verification found for user: {}, type: {}, context: {}",
                        request.getUserId(), request.getVerificationType(), request.getContext());
                return VerificationResponse.builder()
                        .verified(false)
                        .message("No active verification found or verification has expired. Please request a new code.")
                        .nextStep(null)
                        .remainingAttempts(null)
                        .build();
            }

            Verification verification = verificationOpt.get();
            log.info("Found active verification with ID: {} for user: {}", verification.getVerificationId(), request.getUserId());

            // Check if already verified
            if (verification.getVerified()) {
                log.info("Verification already completed for user: {}", request.getUserId());
                return VerificationResponse.builder()
                        .verified(true)
                        .message("Verification already completed")
                        .nextStep(determineNextStep(request.getUserId(), request.getContext()))
                        .build();
            }

            // Check attempt count
            if (verification.getAttemptCount() >= maxAttempts) {
                log.warn("Maximum verification attempts exceeded for user: {}", request.getUserId());
                return VerificationResponse.builder()
                        .verified(false)
                        .message("Maximum verification attempts exceeded. Please request a new code.")
                        .nextStep(null)
                        .remainingAttempts(0)
                        .build();
            }

            // Increment attempt count
            verification.setAttemptCount(verification.getAttemptCount() + 1);
            verificationRepository.save(verification);

            // Verify OTP
            boolean otpMatches = passwordEncoder.matches(request.getOtp(), verification.getOtpHash());

            log.info("OTP verification for user {}: provided='{}', matches={}",
                    request.getUserId(), request.getOtp(), otpMatches);

            if (!otpMatches) {
                int remainingAttempts = maxAttempts - verification.getAttemptCount();
                log.warn("Invalid OTP for user: {}. Remaining attempts: {}", request.getUserId(), remainingAttempts);

                return VerificationResponse.builder()
                        .verified(false)
                        .message("Invalid OTP. Please try again.")
                        .nextStep(null)
                        .remainingAttempts(remainingAttempts)
                        .build();
            }

            // Mark as verified
            verification.setVerified(true);
            verificationRepository.save(verification);

            log.info("OTP verified successfully for user: {}", request.getUserId());

            // AUTO-SEND EMAIL VERIFICATION if SMS was just verified for SIGNUP
            if (verificationType == Verification.VerificationType.SMS &&
                    verificationContext == Verification.VerificationContext.SIGNUP) {

                log.info("SMS verification completed for SIGNUP - auto-sending email verification");
                try {
                    sendEmailVerification(user, Verification.VerificationContext.SIGNUP);
                    log.info("Email verification sent automatically after SMS completion");
                } catch (Exception emailError) {
                    log.error("Failed to auto-send email verification after SMS completion: {}", emailError.getMessage());
                    // Don't fail the SMS verification, but log the error
                }
            }

            // Determine next step based on context and current verification state
            String nextStep = determineNextStep(request.getUserId(), request.getContext());

            return VerificationResponse.builder()
                    .verified(true)
                    .nextStep(nextStep)
                    .message("Verification successful")
                    .remainingAttempts(null)
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error during OTP verification for user: {}: {}", request.getUserId(), e.getMessage(), e);
            return VerificationResponse.builder()
                    .verified(false)
                    .message("Verification failed. Please try again.")
                    .build();
        }
    }

    @Transactional
    public void resendVerification(Integer userId, Verification.VerificationType verificationType,
                                   Verification.VerificationContext context) {
        log.info("Resending verification for user: {} with type: {}", userId, verificationType);

        try {
            // Find user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // For email verification in SIGNUP context, check SMS verification first
            if (verificationType == Verification.VerificationType.EMAIL &&
                    context == Verification.VerificationContext.SIGNUP) {

                boolean smsVerified = isVerificationCompleted(
                        userId,
                        Verification.VerificationType.SMS,
                        Verification.VerificationContext.SIGNUP
                );

                if (!smsVerified) {
                    log.warn("SMS verification not completed for user: {} - cannot resend email verification", userId);
                    throw new IllegalStateException("SMS verification must be completed before email verification");
                }
            }

            // Find active verification to check resend limits
            Optional<Verification> verificationOpt = verificationRepository.findActiveVerification(
                    userId, verificationType, context, LocalDateTime.now()
            );

            if (verificationOpt.isPresent()) {
                Verification verification = verificationOpt.get();

                // Check resend count
                if (verification.getResendCount() >= maxResends) {
                    throw new IllegalStateException("Maximum resend attempts exceeded");
                }

                // Check cooldown period (minimum 30 seconds between resends)
                if (verification.getLastResend() != null &&
                        verification.getLastResend().plusSeconds(30).isAfter(LocalDateTime.now())) {
                    throw new IllegalStateException("Please wait before requesting another code");
                }

                // Update resend count
                verification.setResendCount(verification.getResendCount() + 1);
                verification.setLastResend(LocalDateTime.now());
                verificationRepository.save(verification);
            }

            // Send new verification (this will invalidate existing ones and create new)
            if (verificationType == Verification.VerificationType.SMS) {
                sendSmsVerification(user, context);
            } else {
                sendEmailVerification(user, context);
            }

            log.info("Verification resent successfully for user: {}", userId);

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Re-throw business logic exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during resend verification for user: {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to resend verification: " + e.getMessage(), e);
        }
    }

    @Transactional
    public ChangeVerificationTypeResponse changeVerificationType(ChangeVerificationTypeRequest request) {
        log.info("Changing verification type for user: {} to: {}", request.getUserId(), request.getNewVerificationType());

        try {
            // Find user
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Invalidate existing verifications for this context
            verificationRepository.findAllActiveVerifications(
                    request.getUserId(),
                    mapVerificationContext(request.getContext()),
                    LocalDateTime.now()
            ).forEach(verification -> {
                verification.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expire immediately
                verificationRepository.save(verification);
            });

            // Send new verification
            Verification.VerificationType newType = mapChangeVerificationType(request.getNewVerificationType());
            Verification.VerificationContext context = mapVerificationContext(request.getContext());

            if (newType == Verification.VerificationType.SMS) {
                sendSmsVerification(user, context);
            } else {
                sendEmailVerification(user, context);
            }

            // Return masked contact info
            String maskedContact = newType == Verification.VerificationType.SMS
                    ? maskingUtils.maskPhone(user.getPhone())
                    : maskingUtils.maskEmail(user.getEmail());

            return ChangeVerificationTypeResponse.builder()
                    .maskedContact(maskedContact)
                    .verificationType(newType.name())
                    .message("Verification method changed successfully")
                    .build();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during change verification type for user: {}: {}", request.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to change verification type: " + e.getMessage(), e);
        }
    }

    public boolean isVerificationCompleted(Integer userId, Verification.VerificationType verificationType,
                                           Verification.VerificationContext context) {
        return verificationRepository.findCompletedVerification(userId, verificationType, context).isPresent();
    }

    private String determineNextStep(Integer userId, VerificationRequest.VerificationContext context) {
        if (context == VerificationRequest.VerificationContext.SIGNUP) {
            boolean smsVerified = isVerificationCompleted(
                    userId,
                    Verification.VerificationType.SMS,
                    Verification.VerificationContext.SIGNUP
            );

            boolean emailVerified = isVerificationCompleted(
                    userId,
                    Verification.VerificationType.EMAIL,
                    Verification.VerificationContext.SIGNUP
            );

            log.info("Next step determination for user {}: SMS verified={}, Email verified={}",
                    userId, smsVerified, emailVerified);

            if (smsVerified && !emailVerified) {
                return "EMAIL_VERIFICATION_REQUIRED";
            } else if (smsVerified && emailVerified) {
                return "COMPANY_DETAILS_REQUIRED";
            } else {
                return "SMS_VERIFICATION_REQUIRED";
            }
        }

        return "VERIFICATION_COMPLETED";
    }

    private String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(RANDOM.nextInt(10));
        }
        String generatedOtp = otp.toString();
        log.info("Generated OTP: {}", generatedOtp);
        return generatedOtp;
    }

    private Verification.VerificationType mapVerificationType(VerificationRequest.VerificationType type) {
        return Verification.VerificationType.valueOf(type.name());
    }

    private Verification.VerificationContext mapVerificationContext(VerificationRequest.VerificationContext context) {
        return Verification.VerificationContext.valueOf(context.name());
    }

    private Verification.VerificationContext mapVerificationContext(ChangeVerificationTypeRequest.VerificationContext context) {
        return Verification.VerificationContext.valueOf(context.name());
    }

    private Verification.VerificationType mapChangeVerificationType(ChangeVerificationTypeRequest.NewVerificationType type) {
        return Verification.VerificationType.valueOf(type.name());
    }
}