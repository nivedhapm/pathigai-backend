package com.nivedha.pathigai.auth.services;

import com.nivedha.pathigai.auth.dto.request.SignupRegisterRequest;
import com.nivedha.pathigai.auth.dto.request.SignupCompleteRequest;
import com.nivedha.pathigai.auth.dto.response.SignupRegisterResponse;
import com.nivedha.pathigai.auth.dto.response.SignupCompleteResponse;
import com.nivedha.pathigai.auth.entities.*;
import com.nivedha.pathigai.auth.repositories.CompanyRepository;
import com.nivedha.pathigai.auth.repositories.ProfileRepository;
import com.nivedha.pathigai.auth.repositories.RoleRepository;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.auth.repositories.SessionRepository;
import com.nivedha.pathigai.config.JwtConfig;
//import com.nivedha.pathigai.services.external.RecaptchaService;
import com.nivedha.pathigai.auth.services.utils.MaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SignupService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final ProfileRepository profileRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final MaskingUtils maskingUtils;
    private final JwtConfig jwtConfig;
//    private final RecaptchaService recaptchaService;  // Added RecaptchaService injection

    public SignupRegisterResponse registerUser(SignupRegisterRequest request) {
        log.info("Starting user registration for email: {}", request.getEmail());

//        // Verify reCAPTCHA first
//        boolean recaptchaValid = recaptchaService.verifyRecaptcha(request.getRecaptchaToken(), userIpAddress);
//        if (!recaptchaValid) {
//            throw new IllegalArgumentException("reCAPTCHA verification failed");
//        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Check if phone already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        // Create user entity
        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(false) // Will be enabled after verification
                .userStatus(User.UserStatus.ACTIVE)
                .isTemporaryPassword(false)
                .isCompanyCreator(true) // First user becomes company creator
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created with ID: {}", savedUser.getUserId());

        // Send SMS verification
        verificationService.sendSmsVerification(savedUser, Verification.VerificationContext.SIGNUP);

        return SignupRegisterResponse.builder()
                .userId(savedUser.getUserId())
                .maskedEmail(maskingUtils.maskEmail(request.getEmail()))
                .maskedPhone(maskingUtils.maskPhone(request.getPhone()))
                .nextStep("SMS_VERIFICATION_REQUIRED")
                .message("Account created successfully. Please verify your phone number.")
                .build();


    }

    public SignupCompleteResponse completeSignup(SignupCompleteRequest request) {
        log.info("Completing signup for user ID: {}", request.getUserId());

        // Fetch user normally (old eager fetch method removed)
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalStateException("User account is already completed");
        }

        boolean smsVerified = verificationService.isVerificationCompleted(
                user.getUserId(), Verification.VerificationType.SMS, Verification.VerificationContext.SIGNUP);
        boolean emailVerified = verificationService.isVerificationCompleted(
                user.getUserId(), Verification.VerificationType.EMAIL, Verification.VerificationContext.SIGNUP);
        if (!smsVerified || !emailVerified) {
            throw new IllegalStateException("Both SMS and Email verification must be completed");
        }

        if (companyRepository.existsByCompanyNameIgnoreCase(request.getCompanyName())) {
            throw new IllegalArgumentException("Company name already exists");
        }

        Company company = Company.builder()
                .companyName(request.getCompanyName())
                .industry(request.getIndustry())
                .companyWebsite(request.getCompanyWebsite())
                .build();
        Company savedCompany = companyRepository.save(company);
        log.info("Company created with ID: {}", savedCompany.getCompanyId());

        user.setCompany(savedCompany);
        user.setEnabled(true);

        // Assign single role & profile (ADMIN + SUPER_ADMIN per requirements)
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));
        Profile superAdminProfile = profileRepository.findByName("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN profile not found"));
        user.setPrimaryRole(adminRole);
        user.setPrimaryProfile(superAdminProfile);

        userRepository.save(user);
        log.info("User signup completed for ID: {} with role=ADMIN profile=SUPER_ADMIN", user.getUserId());

        // Generate JWT tokens with embedded profile/role claims for immediate authentication
        String accessToken = jwtConfig.generateAccessToken(user);
        String refreshToken = jwtConfig.generateRefreshToken(user.getUserId(), user.getEmail());

        // Create session record for the new user
        Session session = Session.builder()
                .user(user)
                .jwtToken(accessToken)
                .ipAddress("signup-completion") // Could be improved to pass actual IP
                .userAgent("signup-completion") // Could be improved to pass actual user agent
                .expiresAt(LocalDateTime.now().plusSeconds(jwtConfig.getAccessTokenExpiration() / 1000))
                .isActive(true)
                .build();

        sessionRepository.save(session);
        log.info("Session created for newly signed up user: {}", user.getUserId());

        return SignupCompleteResponse.builder()
                .userId(user.getUserId())
                .companyId(savedCompany.getCompanyId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .jwtToken(accessToken)
                .refreshToken(refreshToken)
                .tokenExpiresIn(jwtConfig.getAccessTokenExpiration())
                .success(true)
                .message("Account created successfully! Welcome to Pathigai!")
                .build();
    }

    public boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean checkPhoneExists(String phone) {
        return userRepository.existsByPhone(phone);
    }
}
