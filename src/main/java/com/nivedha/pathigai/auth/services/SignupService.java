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
//import com.nivedha.pathigai.services.external.RecaptchaService;
import com.nivedha.pathigai.auth.services.utils.MaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final MaskingUtils maskingUtils;
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

        // ONLY THIS LINE CHANGES - use the new repository method
        User user = userRepository.findByIdWithRolesAndProfiles(request.getUserId())
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

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));
        Profile adminProfile = profileRepository.findByName("ADMINISTRATION")
                .orElseThrow(() -> new IllegalStateException("ADMINISTRATION profile not found"));

        // FIXED: Create mutable collections instead of using Set.of()
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        user.setRoles(roles);

        Set<Profile> profiles = new HashSet<>();
        profiles.add(adminProfile);
        user.setProfiles(profiles);

        User updatedUser = userRepository.save(user);
        log.info("User signup completed for ID: {}", updatedUser.getUserId());

        return SignupCompleteResponse.builder()
                .userId(updatedUser.getUserId())
                .companyId(savedCompany.getCompanyId())
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
