package com.nivedha.pathigai.auth.services;

import com.nivedha.pathigai.auth.dto.request.CreateUserRequest;
import com.nivedha.pathigai.auth.dto.response.BulkUploadResponse;
import com.nivedha.pathigai.auth.dto.response.RolesProfilesResponse;
import com.nivedha.pathigai.auth.dto.response.UserProfileResponse;
import com.nivedha.pathigai.auth.entities.Profile;
import com.nivedha.pathigai.auth.entities.Role;
import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.repositories.ProfileRepository;
import com.nivedha.pathigai.auth.repositories.RoleRepository;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IamService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public RolesProfilesResponse getRolesAndProfiles() {
        log.info("Fetching all roles and profiles");

        List<Role> roles = roleRepository.findAll();
        List<Profile> profiles = profileRepository.findAllOrderedByHierarchy();

        List<RolesProfilesResponse.RoleDto> roleDtos = roles.stream()
                .map(role -> RolesProfilesResponse.RoleDto.builder()
                        .roleId(role.getRoleId())
                        .name(role.getName())
                        .build())
                .collect(Collectors.toList());

        List<RolesProfilesResponse.ProfileDto> profileDtos = profiles.stream()
                .map(profile -> RolesProfilesResponse.ProfileDto.builder()
                        .profileId(profile.getProfileId())
                        .name(profile.getName())
                        .description(profile.getDescription())
                        .hierarchyLevel(profile.getHierarchyLevel())
                        .build())
                .collect(Collectors.toList());

        return RolesProfilesResponse.builder()
                .roles(roleDtos)
                .profiles(profileDtos)
                .build();
    }

    public Page<UserProfileResponse> getUsers(String query, int page, int size, Integer companyId) {
        log.info("Fetching users with query: '{}', page: {}, size: {}, companyId: {}", query, page, size, companyId);

        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage;

        if (query != null && !query.trim().isEmpty()) {
            userPage = userRepository.findByCompanyIdAndSearchQuery(companyId, query.trim(), pageable);
        } else {
            userPage = userRepository.findByCompanyIdAndUserStatusNot(companyId, User.UserStatus.DELETED, pageable);
        }

        return userPage.map(this::convertToUserProfileResponse);
    }

    public UserProfileResponse createUser(CreateUserRequest request, Integer createdByUserId) {
        log.info("Creating new user with email: {} by user: {}", request.getEmail(), createdByUserId);

        // Validate user doesn't already exist
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        // Get creator user for validation
        User creator = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Creator user not found"));

        // Validate role-profile mapping
        validateRoleProfileMapping(creator.getProfileName(), request.getRole(), request.getTargetProfile());

        // Get role and profile entities
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.getRole()));

        Profile profile = null;
        if (!"APPLICANT".equals(request.getRole())) {
            profile = profileRepository.findByName(request.getTargetProfile())
                    .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + request.getTargetProfile()));
        }

        // Generate temporary password
        String tempPassword = generateTemporaryPassword();

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .enabled(true)
                .userStatus(User.UserStatus.ACTIVE)
                .isTemporaryPassword(true)
                .isCompanyCreator(false)
                .primaryRole(role)
                .primaryProfile(profile)
                .createdByUser(creator)
                .build();

        // Set company
        user.setCompany(creator.getCompany());

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getUserId());

        // TODO: Send invitation email with temporary password
        if (request.getSendInvitation()) {
            sendInvitationEmail(savedUser, tempPassword);
        }

        return convertToUserProfileResponse(savedUser);
    }

    public UserProfileResponse updateUser(Integer userId, CreateUserRequest request, Integer updatedByUserId) {
        log.info("Updating user ID: {} by user: {}", userId, updatedByUserId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User updater = userRepository.findById(updatedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Updater user not found"));

        // Validate role-profile mapping
        validateRoleProfileMapping(updater.getProfileName(), request.getRole(), request.getTargetProfile());

        // Update role and profile
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.getRole()));

        Profile profile = null;
        if (!"APPLICANT".equals(request.getRole())) {
            profile = profileRepository.findByName(request.getTargetProfile())
                    .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + request.getTargetProfile()));
        }

        // Update user fields
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setPrimaryRole(role);
        user.setPrimaryProfile(profile);

        User savedUser = userRepository.save(user);
        log.info("User updated successfully: {}", userId);

        return convertToUserProfileResponse(savedUser);
    }

    public void softDeleteUser(Integer userId, Integer deletedByUserId) {
        log.info("Soft deleting user ID: {} by user: {}", userId, deletedByUserId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setUserStatus(User.UserStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());
        user.setEnabled(false);

        userRepository.save(user);

        // TODO: Deactivate all sessions for this user
        // sessionRepository.deactivateAllUserSessions(userId);

        log.info("User soft deleted successfully: {}", userId);
    }

    // Add bulk upload functionality to IamService
    public BulkUploadResponse bulkCreateUsers(MultipartFile file, Integer createdByUserId) {
        log.info("Starting bulk user creation by user: {}", createdByUserId);

        User creator = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Creator user not found"));

        List<BulkUploadResponse.ErrorDetail> errors = new ArrayList<>();
        int accepted = 0;
        int rejected = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowIndex = 0;

            for (Row row : sheet) {
                rowIndex++;

                // Skip header row
                if (rowIndex == 1) continue;

                try {
                    // Extract data from row
                    String email = getCellValueAsString(row.getCell(0));
                    String fullName = getCellValueAsString(row.getCell(1));
                    String phone = getCellValueAsString(row.getCell(2));
                    String role = getCellValueAsString(row.getCell(3));
                    String profile = getCellValueAsString(row.getCell(4));

                    // Validate required fields
                    if (email == null || fullName == null || phone == null || role == null) {
                        errors.add(BulkUploadResponse.ErrorDetail.builder()
                                .row(rowIndex)
                                .message("Missing required fields")
                                .build());
                        rejected++;
                        continue;
                    }

                    // Check if user already exists
                    if (userRepository.existsByEmail(email)) {
                        errors.add(BulkUploadResponse.ErrorDetail.builder()
                                .row(rowIndex)
                                .message("User with email " + email + " already exists")
                                .build());
                        rejected++;
                        continue;
                    }

                    // Create user request
                    CreateUserRequest request = new CreateUserRequest();
                    request.setEmail(email);
                    request.setFullName(fullName);
                    request.setPhone(phone);
                    request.setRole(role);
                    request.setTargetProfile(profile);
                    request.setCompanyId(creator.getCompanyId());
                    request.setSendInvitation(true);

                    // Create user
                    createUser(request, createdByUserId);
                    accepted++;

                } catch (Exception e) {
                    errors.add(BulkUploadResponse.ErrorDetail.builder()
                            .row(rowIndex)
                            .message("Error: " + e.getMessage())
                            .build());
                    rejected++;
                }
            }

        } catch (Exception e) {
            log.error("Error processing bulk upload file", e);
            throw new RuntimeException("Error processing file: " + e.getMessage());
        }

        log.info("Bulk upload completed: {} accepted, {} rejected", accepted, rejected);

        return BulkUploadResponse.builder()
                .accepted(accepted)
                .rejected(rejected)
                .errors(errors)
                .build();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    private void validateRoleProfileMapping(String creatorProfile, String targetRole, String targetProfile) {
        // Define role-profile mapping rules
        Map<String, Map<String, List<String>>> roleProfileMappings = Map.of(
            "SUPER_ADMIN", Map.of(
                "ADMIN", List.of("SUPER_ADMIN"),
                "MANAGER", List.of("SUPER_ADMIN", "ADMIN", "MANAGEMENT"),
                "HR", List.of("SUPER_ADMIN", "ADMIN", "MANAGEMENT"),
                "FACULTY", List.of("ADMIN", "MANAGEMENT", "TRAINER"),
                "MENTOR", List.of("TRAINER"),
                "INTERVIEW_PANELIST", List.of("INTERVIEW_PANELIST"),
                "EMPLOYEE", List.of("PLACEMENT"),
                "TRAINEE", List.of("TRAINEE"),
                "APPLICANT", List.of() // No profile for applicant
            ),
            "ADMIN", Map.of(
                "MANAGER", List.of("MANAGEMENT"),
                "HR", List.of("MANAGEMENT"),
                "FACULTY", List.of("TRAINER"),
                "MENTOR", List.of("TRAINER"),
                "INTERVIEW_PANELIST", List.of("INTERVIEW_PANELIST"),
                "EMPLOYEE", List.of("PLACEMENT"),
                "TRAINEE", List.of("TRAINEE"),
                "APPLICANT", List.of()
            ),
            "MANAGEMENT", Map.of(
                "INTERVIEW_PANELIST", List.of("INTERVIEW_PANELIST"),
                "TRAINEE", List.of("TRAINEE"),
                "APPLICANT", List.of()
            )
        );

        if (!roleProfileMappings.containsKey(creatorProfile)) {
            throw new IllegalArgumentException("Creator profile does not have permission to create users");
        }

        Map<String, List<String>> allowedMappings = roleProfileMappings.get(creatorProfile);
        if (!allowedMappings.containsKey(targetRole)) {
            throw new IllegalArgumentException("Creator cannot assign role: " + targetRole);
        }

        List<String> allowedProfiles = allowedMappings.get(targetRole);
        if ("APPLICANT".equals(targetRole)) {
            if (targetProfile != null) {
                throw new IllegalArgumentException("APPLICANT role should not have a profile");
            }
        } else {
            if (!allowedProfiles.contains(targetProfile)) {
                throw new IllegalArgumentException("Invalid role-profile mapping: " + targetRole + " -> " + targetProfile);
            }
        }
    }

    private String generateTemporaryPassword() {
        return "Temp" + UUID.randomUUID().toString().substring(0, 8) + "!";
    }

    private void sendInvitationEmail(User user, String tempPassword) {
        log.info("Sending invitation email to: {}", user.getEmail());
        // TODO: Implement email sending logic
        // This should send an email with:
        // - Welcome message
        // - Temporary password
        // - Link to reset password
        log.info("Invitation email would be sent with temp password: {}", tempPassword);
    }

    private UserProfileResponse convertToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRoleName())
                .profile(user.getProfileName())
                .profileLevel(user.getPrimaryProfile() != null ? user.getPrimaryProfile().getHierarchyLevel() : null)
                .companyId(user.getCompanyId())
                .companyName(user.getCompanyName())
                .isCompanyCreator(user.getIsCompanyCreator())
                .isTemporaryPassword(user.getIsTemporaryPassword())
                .createdBy(user.getCreatedByUser() != null ? user.getCreatedByUser().getFullName() : null)
                .build();
    }
}
