package com.nivedha.pathigai.user.services;

import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.entities.Role;
import com.nivedha.pathigai.auth.entities.Profile;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.auth.repositories.RoleRepository;
import com.nivedha.pathigai.auth.repositories.ProfileRepository;
import com.nivedha.pathigai.user.dto.request.CreateUserRequest;
import com.nivedha.pathigai.user.dto.request.BulkCreateUsersRequest;
import com.nivedha.pathigai.user.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEmailService emailService;

    // Role-Profile mappings based on your frontend requirements
    private static final Map<String, List<String>> ROLE_PROFILE_MAPPINGS = Map.of(
        "SUPER_ADMIN", List.of("ADMIN", "MANAGER", "HR"),
        "ADMIN", List.of("MANAGER", "HR", "FACULTY"),
        "MANAGEMENT", List.of("HR", "MANAGER"),
        "TRAINER", List.of("FACULTY", "MENTOR"),
        "INTERVIEW_PANELIST", List.of("INTERVIEW_PANELIST"),
        "PLACEMENT", List.of("EMPLOYEE"),
        "TRAINEE", List.of("TRAINEE")
    );

    /**
     * Create a single user
     */
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request, User createdBy) {
        try {
            log.info("🆕 Creating new user: {} by user: {}", request.getEmail(), createdBy.getEmail());

            // Validate the request
            validateUserCreationRequest(request, createdBy);

            // Check for duplicate email
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                log.warn("❌ Email already exists: {}", request.getEmail());
                return CreateUserResponse.failure("Email address already exists");
            }

            // Check for duplicate phone
            if (userRepository.findByPhone(request.getPhone()).isPresent()) {
                log.warn("❌ Phone number already exists: {}", request.getPhone());
                return CreateUserResponse.failure("Phone number already exists");
            }

            // Get role and profile entities
            Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + request.getRole()));

            Profile profile = profileRepository.findByName(request.getProfile())
                .orElseThrow(() -> new IllegalArgumentException("Invalid profile: " + request.getProfile()));

            // Create new user entity
            User newUser = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(User.Gender.valueOf(request.getGender()))
                .workLocation(request.getWorkLocation())
                .passwordHash(passwordEncoder.encode(request.getTemporaryPassword()))
                .enabled(request.getIsActive())
                .userStatus(User.UserStatus.ACTIVE)
                .isTemporaryPassword(true)
                .isCompanyCreator(false)
                .company(createdBy.getCompany())
                .createdByUser(createdBy)
                .primaryRole(role)
                .primaryProfile(profile)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

            // Save the user first in a separate transaction
            User savedUser = userRepository.save(newUser);
            log.info("✅ User created successfully: {} with ID: {}", savedUser.getEmail(), savedUser.getUserId());

            // Convert to response DTO
            UserResponse userResponse = convertToUserResponse(savedUser);

            // Send welcome email AFTER user is successfully saved (separate transaction)
            // This prevents email failures from rolling back user creation
            sendWelcomeEmailAsync(savedUser, request.getTemporaryPassword(), createdBy);

            return CreateUserResponse.success(savedUser.getUserId(), userResponse);

        } catch (IllegalArgumentException e) {
            log.error("❌ Validation error creating user: {}", e.getMessage());
            return CreateUserResponse.failure(e.getMessage());
        } catch (Exception e) {
            log.error("❌ Error creating user: {}", e.getMessage(), e);
            return CreateUserResponse.failure("Failed to create user: " + e.getMessage());
        }
    }

    /**
     * Send welcome email asynchronously after user creation
     * This method is separate to prevent email failures from affecting user creation
     */
    private void sendWelcomeEmailAsync(User savedUser, String temporaryPassword, User createdBy) {
        try {
            log.info("📧 Initiating welcome email sending for: {}", savedUser.getEmail());
            emailService.sendWelcomeEmail(savedUser, temporaryPassword, createdBy);
        } catch (Exception emailError) {
            log.error("❌ Failed to send welcome email to {}: {}", savedUser.getEmail(), emailError.getMessage(), emailError);
            // Email failure should not affect user creation - just log the error
        }
    }

    /**
     * Bulk create users from CSV
     */
    @Transactional
    public BulkCreateUsersResponse bulkCreateUsers(BulkCreateUsersRequest request, User createdBy) {
        log.info("📄 Starting bulk user creation for {} users by: {}",
                request.getUsers().size(), createdBy.getEmail());

        List<BulkCreateUsersResponse.SuccessfulUser> successfulUsers = new ArrayList<>();
        List<BulkCreateUsersResponse.FailedUser> failedUsers = new ArrayList<>();

        for (int i = 0; i < request.getUsers().size(); i++) {
            CreateUserRequest userRequest = request.getUsers().get(i);

            try {
                // Create individual user
                CreateUserResponse result = createUser(userRequest, createdBy);

                if (result.isSuccess()) {
                    successfulUsers.add(BulkCreateUsersResponse.SuccessfulUser.builder()
                        .id(result.getUserId())
                        .fullName(userRequest.getFullName())
                        .email(userRequest.getEmail())
                        .build());
                } else {
                    failedUsers.add(BulkCreateUsersResponse.FailedUser.builder()
                        .rowIndex(i + 1)
                        .data(userRequest)
                        .errors(List.of(result.getMessage()))
                        .build());
                }
            } catch (Exception e) {
                log.error("❌ Error creating user at row {}: {}", i + 1, e.getMessage());
                failedUsers.add(BulkCreateUsersResponse.FailedUser.builder()
                    .rowIndex(i + 1)
                    .data(userRequest)
                    .errors(List.of("Unexpected error: " + e.getMessage()))
                    .build());
            }
        }

        BulkCreateUsersResponse.BulkResults results = BulkCreateUsersResponse.BulkResults.builder()
            .totalSubmitted(request.getUsers().size())
            .successCount(successfulUsers.size())
            .errorCount(failedUsers.size())
            .successfulUsers(successfulUsers)
            .failedUsers(failedUsers)
            .build();

        log.info("🎉 Bulk creation completed: {} successful, {} failed",
                successfulUsers.size(), failedUsers.size());

        return BulkCreateUsersResponse.success(results);
    }

    /**
     * Get profiles that the current user can create
     */
    public List<ProfileResponse> getAllowedCreationProfiles(User currentUser) {
        if (currentUser.getPrimaryProfile() == null) {
            return Collections.emptyList();
        }

        String currentProfileName = currentUser.getPrimaryProfile().getName();
        Integer currentProfileLevel = currentUser.getPrimaryProfile().getHierarchyLevel();

        // Users can create profiles at their level or lower (higher hierarchy level number)
        List<Profile> allowedProfiles = profileRepository.findByHierarchyLevelGreaterThanEqualOrderByHierarchyLevel(currentProfileLevel);

        return allowedProfiles.stream()
            .map(this::convertToProfileResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get roles allowed for a specific profile
     */
    public List<RoleResponse> getAllowedRolesForProfile(String profileName) {
        List<String> allowedRoleNames = ROLE_PROFILE_MAPPINGS.getOrDefault(profileName, Collections.emptyList());

        List<Role> roles = roleRepository.findByNameIn(allowedRoleNames);

        return roles.stream()
            .map(this::convertToRoleResponse)
            .collect(Collectors.toList());
    }

    /**
     * Validate email uniqueness
     */
    public boolean isEmailAvailable(String email) {
        return userRepository.findByEmail(email).isEmpty();
    }

    /**
     * Validate phone uniqueness
     */
    public boolean isPhoneAvailable(String phone) {
        return userRepository.findByPhone(phone).isEmpty();
    }

    /**
     * Validate user creation request
     */
    private void validateUserCreationRequest(CreateUserRequest request, User createdBy) {
        // Validate age (minimum 13 years)
        if (!request.isValidAge()) {
            throw new IllegalArgumentException("User must be at least 13 years old");
        }

        // Validate profile hierarchy (cannot create higher level profiles)
        Profile targetProfile = profileRepository.findByName(request.getProfile())
            .orElseThrow(() -> new IllegalArgumentException("Invalid profile: " + request.getProfile()));

        if (createdBy.getPrimaryProfile() != null) {
            Integer creatorLevel = createdBy.getPrimaryProfile().getHierarchyLevel();
            Integer targetLevel = targetProfile.getHierarchyLevel();

            if (targetLevel < creatorLevel) {
                throw new IllegalArgumentException("Cannot create users with higher privilege level than your own");
            }
        }

        // Validate role-profile compatibility
        List<String> allowedRoles = ROLE_PROFILE_MAPPINGS.getOrDefault(request.getProfile(), Collections.emptyList());
        if (!allowedRoles.contains(request.getRole())) {
            throw new IllegalArgumentException("Role " + request.getRole() + " is not compatible with profile " + request.getProfile());
        }
    }

    /**
     * Get all users in the company
     */
    public List<UserResponse> getAllUsers(User currentUser) {
        try {
            log.info("📋 Fetching all users for company: {}",
                currentUser.getCompany() != null ? currentUser.getCompany().getCompanyName() : "Unknown");

            // Fetch all users from the same company
            List<User> users = userRepository.findByCompanyCompanyIdAndUserStatus(
                currentUser.getCompany().getCompanyId(),
                User.UserStatus.ACTIVE
            );

            // Convert to UserResponse DTOs
            List<UserResponse> userResponses = users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());

            log.info("✅ Found {} users in company", userResponses.size());
            return userResponses;

        } catch (Exception e) {
            log.error("❌ Error fetching users: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Find user by email (helper method for authentication)
     */
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmailAndUserStatusWithProfileAndRole(email, User.UserStatus.ACTIVE);
    }

    /**
     * Convert User entity to UserResponse DTO
     */
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
            .userId(user.getUserId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .dateOfBirth(user.getDateOfBirth())
            .gender(user.getGender() != null ? user.getGender().name() : null)
            .workLocation(user.getWorkLocation())
            .role(user.getPrimaryRole() != null ? user.getPrimaryRole().getName() : null)
            .profile(user.getPrimaryProfile() != null ? user.getPrimaryProfile().getName() : null)
            .isActive(user.getEnabled())
            .mustChangePassword(user.getIsTemporaryPassword())
            .emailVerified(user.getEmailVerified())
            .phoneVerified(user.getPhoneVerified())
            .companyName(user.getCompany() != null ? user.getCompany().getCompanyName() : null)
            .createdByName(user.getCreatedByUser() != null ? user.getCreatedByUser().getFullName() : null)
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    /**
     * Convert Profile entity to ProfileResponse DTO
     */
    private ProfileResponse convertToProfileResponse(Profile profile) {
        return ProfileResponse.builder()
            .profileId(profile.getProfileId())
            .key(profile.getName())
            .label(profile.getName().replace("_", " "))
            .level(profile.getHierarchyLevel())
            .description(profile.getDescription())
            .build();
    }

    /**
     * Convert Role entity to RoleResponse DTO
     */
    private RoleResponse convertToRoleResponse(Role role) {
        return RoleResponse.builder()
            .roleId(role.getRoleId())
            .key(role.getName())
            .label(role.getName().replace("_", " "))
            .build();
    }
}
