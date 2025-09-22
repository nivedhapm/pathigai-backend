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
import com.nivedha.pathigai.auth.services.utils.MaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SignupService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final MaskingUtils maskingUtils;

    public SignupRegisterResponse registerUser(SignupRegisterRequest request) {
        log.info("Starting user registration for email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Check if phone already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        // Get default role and profile for new signups
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found in database"));

        Profile superAdminProfile = profileRepository.findByName("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN profile not found in database"));

        // Create user entity with default role and profile
        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(false) // Will be enabled after verification
                .userStatus(User.UserStatus.ACTIVE)
                .isTemporaryPassword(false)
                .isCompanyCreator(true) // First user becomes company creator
                .primaryRole(adminRole)
                .primaryProfile(superAdminProfile)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created with ID: {} with ADMIN role and SUPER_ADMIN profile", savedUser.getUserId());

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

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getEnabled()) {
            throw new IllegalStateException("User account is already completed");
        }

        boolean smsVerified = verificationService.isVerificationCompleted(
                user.getUserId(),
                Verification.VerificationType.SMS,
                Verification.VerificationContext.SIGNUP
        );

        boolean emailVerified = verificationService.isVerificationCompleted(
                user.getUserId(),
                Verification.VerificationType.EMAIL,
                Verification.VerificationContext.SIGNUP
        );

        if (!smsVerified || !emailVerified) {
            throw new IllegalStateException("SMS and Email verification must be completed before signup completion");
        }

        // Create company
        Company company = Company.builder()
                .companyName(request.getCompanyName())
                .industry(request.getIndustry())
                .companyWebsite(request.getWebsite())
                .build();

        Company savedCompany = companyRepository.save(company);
        log.info("Company created with ID: {}", savedCompany.getCompanyId());

        // Update user with company
        user.setCompany(savedCompany);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);

        userRepository.save(user);
        log.info("User signup completed successfully for user ID: {}", user.getUserId());

        return SignupCompleteResponse.builder()
                .success(true)
                .message("Signup completed successfully")
                .build();
    }
}
