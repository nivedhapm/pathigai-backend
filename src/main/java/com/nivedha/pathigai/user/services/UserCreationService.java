package com.nivedha.pathigai.user.services;

import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.entities.Role;
import com.nivedha.pathigai.auth.entities.Profile;
import com.nivedha.pathigai.auth.entities.Company;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.auth.repositories.RoleRepository;
import com.nivedha.pathigai.auth.repositories.ProfileRepository; // Changed: use auth module repository
import com.nivedha.pathigai.profile.services.RoleProfileMappingService;
import com.nivedha.pathigai.user.dto.request.CreateUserRequest;
import com.nivedha.pathigai.user.dto.response.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class UserCreationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProfileRepository profileRepository;
    private final RoleProfileMappingService roleProfileMappingService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Transactional
    public UserProfileResponse createUser(CreateUserRequest request, String creatorEmail) {
        // Get creator user to validate permissions
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("Creator not found: " + creatorEmail));

        // Validate creator can create this role-profile combination
        validateCreatorPermissions(creator, request.getRole(), request.getTargetProfile());

        // Validate role-profile mapping
        String creatorProfile = creator.getPrimaryProfile() != null ?
                creator.getPrimaryProfile().getName() : null; // Fixed method name

        if (!roleProfileMappingService.validateRoleProfileMapping(creatorProfile, request.getRole(), request.getTargetProfile())) {
            throw new RuntimeException("Invalid role-profile mapping for creator profile: " + creatorProfile);
        }

        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists with email: " + request.getEmail());
        }

        // Get role entity
        Role role = roleRepository.findByName(request.getRole()) // Fixed: removed AndIsActiveTrue
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRole()));

        // Get profile entity (can be null for APPLICANT)
        Profile profile = null;
        if (request.getTargetProfile() != null) {
            profile = profileRepository.findByName(request.getTargetProfile()) // Fixed method name
                    .orElseThrow(() -> new RuntimeException("Profile not found: " + request.getTargetProfile()));
        }

        // Generate temporary password
        String temporaryPassword = generateTemporaryPassword();

        // Create user entity
        User newUser = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(temporaryPassword))
                .enabled(true)
                .isTemporaryPassword(true)
                .isCompanyCreator(false)
                .company(creator.getCompany()) // Same company as creator
                .primaryRole(role)
                .primaryProfile(profile)
                // .createdBy(creator) // Commented out if this field doesn't exist
                .build();

        // Save user
        User savedUser = userRepository.save(newUser);

        // Send invitation if requested
        if (request.getSendInvitation()) {
            sendUserInvitation(savedUser, temporaryPassword);
        }

        return userService.getUserProfile(savedUser.getEmail());
    }

    private void validateCreatorPermissions(User creator, String targetRole, String targetProfile) {
        Profile creatorProfile = creator.getPrimaryProfile();

        if (creatorProfile == null && !"APPLICANT".equals(targetRole)) {
            throw new RuntimeException("Creator must have a profile to create users with profiles");
        }

        // Implement specific permission validation based on your business rules
        // Basic hierarchy validation - creator must have higher or equal level
        if (targetProfile != null && creatorProfile != null) {
            Profile targetProfileEntity = profileRepository.findByName(targetProfile) // Fixed method name
                    .orElseThrow(() -> new RuntimeException("Target profile not found: " + targetProfile));

            if (creatorProfile.getHierarchyLevel() > targetProfileEntity.getHierarchyLevel()) {
                throw new RuntimeException("Insufficient permissions to create user with profile: " + targetProfile);
            }
        }
    }

    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendUserInvitation(User user, String temporaryPassword) {
        // Implement email invitation logic
        // This could use your existing mail service
        System.out.println("Sending invitation to: " + user.getEmail() + " with temporary password: " + temporaryPassword);
    }
}
