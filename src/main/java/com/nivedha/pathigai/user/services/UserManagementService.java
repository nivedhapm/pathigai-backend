package com.nivedha.pathigai.user.services;

import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.entities.Role;
import com.nivedha.pathigai.auth.entities.Profile;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.auth.repositories.RoleRepository;
import com.nivedha.pathigai.auth.repositories.ProfileRepository;
import com.nivedha.pathigai.user.dto.request.CreateUserRequest;
import com.nivedha.pathigai.user.dto.request.BulkCreateUsersRequest;
import com.nivedha.pathigai.user.dto.request.UpdateUserRequest;
import com.nivedha.pathigai.user.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
            List<String> validationErrors = validateUserCreationRequest(request, createdBy);
            if (!validationErrors.isEmpty()) {
                log.warn("❌ Validation failed for user: {} - Errors: {}", request.getEmail(), validationErrors);
                return CreateUserResponse.failure("Validation failed: " + String.join(", ", validationErrors));
            }

            // Create the user
            User savedUser = createUserEntity(request, createdBy);
            log.info("✅ User created successfully: {} with ID: {}", savedUser.getEmail(), savedUser.getUserId());

            // Convert to response DTO
            UserResponse userResponse = convertToUserResponse(savedUser);

            // Send email invitation asynchronously
            try {
                boolean emailSent = emailService.sendUserInvitation(savedUser, request.getTemporaryPassword());
                log.info("📧 Email invitation sent to {}: {}", savedUser.getEmail(), emailSent ? "SUCCESS" : "FAILED");
            } catch (Exception e) {
                log.error("❌ Failed to send email invitation to {}: {}", savedUser.getEmail(), e.getMessage());
            }

            return CreateUserResponse.success("User created successfully", savedUser.getUserId(), userResponse);

        } catch (Exception e) {
            log.error("❌ Error creating user {}: {}", request.getEmail(), e.getMessage(), e);
            return CreateUserResponse.failure("Failed to create user: " + e.getMessage());
        }
    }

    /**
     * Bulk create users with partial success handling
     * Creates valid users and skips invalid ones
     */
    @Transactional
    public BulkCreateUsersResponse bulkCreateUsers(BulkCreateUsersRequest request, User createdBy) {
        try {
            log.info("📄 Starting bulk user creation for {} users by: {}",
                    request.getUsers().size(), createdBy.getEmail());

            List<BulkCreateUsersResponse.UserCreationResult> successfulUsers = new ArrayList<>();
            List<BulkCreateUsersResponse.UserCreationError> failedUsers = new ArrayList<>();

            // Process each user
            for (int i = 0; i < request.getUsers().size(); i++) {
                CreateUserRequest userRequest = request.getUsers().get(i);
                int rowIndex = i + 1; // Start from 1 for display

                try {
                    log.debug("🔄 Processing user {}/{}: {}", rowIndex, request.getUsers().size(), userRequest.getEmail());

                    // Validate the user request
                    List<String> validationErrors = validateUserCreationRequest(userRequest, createdBy);
                    if (!validationErrors.isEmpty()) {
                        log.warn("❌ Validation failed for row {} ({}): {}", rowIndex, userRequest.getEmail(), validationErrors);

                        failedUsers.add(BulkCreateUsersResponse.UserCreationError.builder()
                                .rowIndex(rowIndex)
                                .fullName(userRequest.getFullName())
                                .email(userRequest.getEmail())
                                .errors(validationErrors)
                                .originalData(userRequest)
                                .build());
                        continue;
                    }

                    // Create the user
                    User savedUser = createUserEntity(userRequest, createdBy);
                    log.debug("✅ User created successfully: {} (row {})", savedUser.getEmail(), rowIndex);

                    // Send email invitation asynchronously
                    boolean emailSent = false;
                    try {
                        emailSent = emailService.sendUserInvitation(savedUser, userRequest.getTemporaryPassword());
                        log.debug("📧 Email invitation for {}: {}", savedUser.getEmail(), emailSent ? "SENT" : "FAILED");
                    } catch (Exception e) {
                        log.warn("⚠️ Failed to send email to {}: {}", savedUser.getEmail(), e.getMessage());
                    }

                    successfulUsers.add(BulkCreateUsersResponse.UserCreationResult.builder()
                            .userId(savedUser.getUserId())
                            .fullName(savedUser.getFullName())
                            .email(savedUser.getEmail())
                            .role(savedUser.getPrimaryRole().getName())
                            .profile(savedUser.getPrimaryProfile().getName())
                            .emailSent(emailSent)
                            .build());

                } catch (Exception e) {
                    log.error("❌ Error processing user at row {} ({}): {}", rowIndex, userRequest.getEmail(), e.getMessage());

                    failedUsers.add(BulkCreateUsersResponse.UserCreationError.builder()
                            .rowIndex(rowIndex)
                            .fullName(userRequest.getFullName())
                            .email(userRequest.getEmail())
                            .errors(List.of("System error: " + e.getMessage()))
                            .originalData(userRequest)
                            .build());
                }
            }

            // Create results summary
            BulkCreateUsersResponse.BulkCreateResults results = BulkCreateUsersResponse.BulkCreateResults.builder()
                    .totalSubmitted(request.getUsers().size())
                    .successCount(successfulUsers.size())
                    .errorCount(failedUsers.size())
                    .successfulUsers(successfulUsers)
                    .failedUsers(failedUsers)
                    .build();

            log.info("🎉 Bulk creation completed: {} successful, {} failed out of {} total",
                    successfulUsers.size(), failedUsers.size(), request.getUsers().size());

            return BulkCreateUsersResponse.success(results);

        } catch (Exception e) {
            log.error("❌ Error in bulk user creation: {}", e.getMessage(), e);
            return BulkCreateUsersResponse.failure("Bulk creation failed: " + e.getMessage());
        }
    }

    /**
     * Get users for the company with search and filtering
     */
    public UserListResponse getUsers(String search, String role, String profile, int page, int size, User currentUser) {
        try {
            log.info("📋 Getting users for company {} - search: '{}', role: '{}', profile: '{}'",
                    currentUser.getCompany().getCompanyId(), search, role, profile);

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<User> userPage;

            if (search != null && !search.trim().isEmpty()) {
                userPage = userRepository.findByCompanyAndSearchCriteria(
                        currentUser.getCompany().getCompanyId(), search.trim(), role, profile, pageable);
            } else {
                userPage = userRepository.findByCompanyAndFilters(
                        currentUser.getCompany().getCompanyId(), role, profile, pageable);
            }

            List<UserResponse> userResponses = userPage.getContent().stream()
                    .map(this::convertToUserResponse)
                    .collect(Collectors.toList());

            return UserListResponse.success(userResponses, userPage.getTotalElements(), page, size);

        } catch (Exception e) {
            log.error("❌ Error getting users: {}", e.getMessage(), e);
            return UserListResponse.failure("Failed to retrieve users: " + e.getMessage());
        }
    }

    /**
     * Update user
     */
    @Transactional
    public CreateUserResponse updateUser(Long userId, UpdateUserRequest request, User updatedBy) {
        try {
            log.info("🔄 Updating user {} by user: {}", userId, updatedBy.getEmail());

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

            // Check if updater has permission to update this user
            if (!canManageUser(updatedBy, user)) {
                return CreateUserResponse.failure("Access denied. You do not have permission to update this user.");
            }

            // Update user fields
            user.setFullName(request.getFullName());
            user.setPhone(request.getPhone());
            user.setDateOfBirth(request.getDateOfBirth());
            user.setGender(request.getGender() != null ? User.Gender.valueOf(request.getGender()) : null);
            user.setWorkLocation(request.getWorkLocation());
            user.setEnabled(request.isActive());

            // Update role and profile if changed
            if (!user.getPrimaryRole().getName().equals(request.getRole())) {
                Role newRole = roleRepository.findByName(request.getRole())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + request.getRole()));
                user.setPrimaryRole(newRole);
            }

            if (!user.getPrimaryProfile().getName().equals(request.getProfile())) {
                Profile newProfile = profileRepository.findByName(request.getProfile())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid profile: " + request.getProfile()));
                user.setPrimaryProfile(newProfile);
            }

            User savedUser = userRepository.save(user);
            UserResponse userResponse = convertToUserResponse(savedUser);

            log.info("✅ User updated successfully: {}", savedUser.getEmail());
            return CreateUserResponse.success("User updated successfully", savedUser.getUserId(), userResponse);

        } catch (Exception e) {
            log.error("❌ Error updating user {}: {}", userId, e.getMessage(), e);
            return CreateUserResponse.failure("Failed to update user: " + e.getMessage());
        }
    }

    /**
     * Delete user (soft delete)
     */
    @Transactional
    public boolean deleteUser(Long userId, User deletedBy) {
        try {
            log.info("🗑️ Deleting user {} by user: {}", userId, deletedBy.getEmail());

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

            // Check if deleter has permission to delete this user
            if (!canManageUser(deletedBy, user)) {
                log.warn("❌ Access denied for user {} to delete user {}", deletedBy.getEmail(), user.getEmail());
                return false;
            }

            // Soft delete
            user.setUserStatus(User.UserStatus.DELETED);
            user.setDeletedAt(LocalDateTime.now());
            user.setEnabled(false);

            userRepository.save(user);
            log.info("✅ User deleted successfully: {}", user.getEmail());
            return true;

        } catch (Exception e) {
            log.error("❌ Error deleting user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validate user creation request
     */
    private List<String> validateUserCreationRequest(CreateUserRequest request, User createdBy) {
        List<String> errors = new ArrayList<>();

        try {
            // Check age validation (minimum 13 years)
            if (request.getDateOfBirth() != null) {
                int age = LocalDate.now().getYear() - request.getDateOfBirth().getYear();
                if (age < 13) {
                    errors.add("User must be at least 13 years old");
                }
            }

            // Check for duplicate email
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                errors.add("Email address already exists");
            }

            // Check for duplicate phone
            if (userRepository.findByPhone(request.getPhone()).isPresent()) {
                errors.add("Phone number already exists");
            }

            // Validate role and profile combination
            if (!ROLE_PROFILE_MAPPINGS.getOrDefault(request.getProfile(), Collections.emptyList())
                    .contains(request.getRole())) {
                errors.add("Invalid role-profile combination");
            }

            // Check if creator can create this profile
            if (!canCreateProfile(createdBy, request.getProfile())) {
                errors.add("You do not have permission to create users with this profile");
            }

            // Validate role exists
            if (!roleRepository.findByName(request.getRole()).isPresent()) {
                errors.add("Invalid role: " + request.getRole());
            }

            // Validate profile exists
            if (!profileRepository.findByName(request.getProfile()).isPresent()) {
                errors.add("Invalid profile: " + request.getProfile());
            }

        } catch (Exception e) {
            errors.add("Validation error: " + e.getMessage());
        }

        return errors;
    }

    /**
     * Create user entity from request
     */
    private User createUserEntity(CreateUserRequest request, User createdBy) {
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + request.getRole()));

        Profile profile = profileRepository.findByName(request.getProfile())
                .orElseThrow(() -> new IllegalArgumentException("Invalid profile: " + request.getProfile()));

        User newUser = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender() != null ? User.Gender.valueOf(request.getGender()) : null)
                .workLocation(request.getWorkLocation())
                .passwordHash(passwordEncoder.encode(request.getTemporaryPassword()))
                .enabled(request.isActive())
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

        return userRepository.save(newUser);
    }

    /**
     * Convert User entity to UserResponse DTO
     */
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getPrimaryRole() != null ? user.getPrimaryRole().getName() : null)
                .profile(user.getPrimaryProfile() != null ? user.getPrimaryProfile().getName() : null)
                .isActive(user.getEnabled())
                .mustChangePassword(user.getIsTemporaryPassword())
                .companyName(user.getCompany() != null ? user.getCompany().getCompanyName() : null)
                .createdByName(user.getCreatedByUser() != null ? user.getCreatedByUser().getFullName() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Get profiles that current user can create
     */
    public List<ProfileResponse> getAllowedCreationProfiles(User currentUser) {
        try {
            String currentUserProfile = currentUser.getPrimaryProfile().getName();

            // Get all profiles the current user can create based on hierarchy
            List<Profile> allowedProfiles = new ArrayList<>();

            switch (currentUserProfile) {
                case "SUPER_ADMIN":
                    allowedProfiles = profileRepository.findAll();
                    break;
                case "ADMIN":
                    allowedProfiles = profileRepository.findByNameIn(
                            List.of("MANAGEMENT", "TRAINER", "INTERVIEW_PANELIST", "PLACEMENT", "TRAINEE"));
                    break;
                case "MANAGEMENT":
                    allowedProfiles = profileRepository.findByNameIn(
                            List.of("TRAINEE", "INTERVIEW_PANELIST"));
                    break;
                default:
                    allowedProfiles = Collections.emptyList();
            }

            return allowedProfiles.stream()
                    .map(this::convertToProfileResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Error getting allowed profiles for user {}: {}", currentUser.getEmail(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get roles for a specific profile
     */
    public List<RoleResponse> getAllowedRolesForProfile(String profileName) {
        try {
            List<String> allowedRoleNames = ROLE_PROFILE_MAPPINGS.getOrDefault(profileName, Collections.emptyList());

            List<Role> roles = roleRepository.findByNameIn(allowedRoleNames);

            return roles.stream()
                    .map(this::convertToRoleResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Error getting roles for profile {}: {}", profileName, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Check if current user can create specified profile
     */
    private boolean canCreateProfile(User currentUser, String targetProfile) {
        String currentUserProfile = currentUser.getPrimaryProfile().getName();

        switch (currentUserProfile) {
            case "SUPER_ADMIN":
                return true;
            case "ADMIN":
                return List.of("MANAGEMENT", "TRAINER", "INTERVIEW_PANELIST", "PLACEMENT", "TRAINEE")
                        .contains(targetProfile);
            case "MANAGEMENT":
                return List.of("TRAINEE", "INTERVIEW_PANELIST").contains(targetProfile);
            default:
                return false;
        }
    }

    /**
     * Check if current user can manage (update/delete) target user
     */
    private boolean canManageUser(User currentUser, User targetUser) {
        // Users can only manage users in their company
        if (!currentUser.getCompany().equals(targetUser.getCompany())) {
            return false;
        }

        // Company creators can manage all users in their company
        if (currentUser.getIsCompanyCreator()) {
            return true;
        }

        // Check profile hierarchy
        String currentUserProfile = currentUser.getPrimaryProfile().getName();
        String targetUserProfile = targetUser.getPrimaryProfile().getName();

        switch (currentUserProfile) {
            case "SUPER_ADMIN":
                return true;
            case "ADMIN":
                return !List.of("SUPER_ADMIN", "ADMIN").contains(targetUserProfile);
            case "MANAGEMENT":
                return List.of("TRAINEE", "INTERVIEW_PANELIST", "PLACEMENT").contains(targetUserProfile);
            default:
                return false;
        }
    }

    /**
     * Convert Profile entity to ProfileResponse DTO
     */
    private ProfileResponse convertToProfileResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getProfileId())
                .name(profile.getName())
                .label(formatProfileLabel(profile.getName()))
                .description(profile.getDescription())
                .hierarchyLevel(profile.getHierarchyLevel())
                .build();
    }

    /**
     * Convert Role entity to RoleResponse DTO
     */
    private RoleResponse convertToRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getRoleId())
                .name(role.getName())
                .label(formatRoleLabel(role.getName()))
                .build();
    }

    /**
     * Format profile name for display
     */
    private String formatProfileLabel(String profileName) {
        return switch (profileName) {
            case "SUPER_ADMIN" -> "Super Admin";
            case "ADMIN" -> "Admin";
            case "MANAGEMENT" -> "Management";
            case "TRAINER" -> "Trainer";
            case "INTERVIEW_PANELIST" -> "Interview Panelist";
            case "PLACEMENT" -> "Placement";
            case "TRAINEE" -> "Trainee";
            default -> profileName;
        };
    }

    /**
     * Format role name for display
     */
    private String formatRoleLabel(String roleName) {
        return switch (roleName) {
            case "ADMIN" -> "Admin";
            case "MANAGER" -> "Manager";
            case "HR" -> "HR";
            case "FACULTY" -> "Faculty";
            case "MENTOR" -> "Mentor";
            case "INTERVIEW_PANELIST" -> "Interview Panelist";
            case "EMPLOYEE" -> "Employee";
            case "TRAINEE" -> "Trainee";
            case "APPLICANT" -> "Applicant";
            default -> roleName;
        };
    }

    /**
     * Validate email uniqueness
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.findByEmail(email).isPresent();
    }

    /**
     * Validate phone uniqueness
     */
    public boolean isPhoneAvailable(String phone) {
        return !userRepository.findByPhone(phone).isPresent();
    }
}
