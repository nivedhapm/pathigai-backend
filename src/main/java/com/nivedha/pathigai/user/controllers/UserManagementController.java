package com.nivedha.pathigai.user.controllers;

import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.user.dto.request.CreateUserRequest;
import com.nivedha.pathigai.user.dto.request.BulkCreateUsersRequest;
import com.nivedha.pathigai.user.dto.request.UpdateUserRequest;
import com.nivedha.pathigai.user.dto.response.*;
import com.nivedha.pathigai.user.services.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "https://pathigai.vercel.app"})
public class UserManagementController {

    private final UserManagementService userManagementService;

    /**
     * Create a single user
     * POST /api/v1/users/create
     */
    @PostMapping("/create")
    public ResponseEntity<CreateUserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("🆕 Creating user request received for: {}", request.getEmail());

            // Get current user from UserDetails (email)
            User currentUser = getCurrentUser(userDetails);

            CreateUserResponse response = userManagementService.createUser(request, currentUser);

            if (response.isSuccess()) {
                log.info("✅ User created successfully: {}", request.getEmail());
                return ResponseEntity.ok(response);
            } else {
                log.warn("❌ User creation failed: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("❌ Error creating user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CreateUserResponse.failure("Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Bulk create users from CSV
     * POST /api/v1/users/bulk-create
     */
    @PostMapping("/bulk-create")
    public ResponseEntity<BulkCreateUsersResponse> bulkCreateUsers(
            @Valid @RequestBody BulkCreateUsersRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("📄 Bulk create users request received for {} users", request.getUsers().size());

            // Get current user from UserDetails
            User currentUser = getCurrentUser(userDetails);

            BulkCreateUsersResponse response = userManagementService.bulkCreateUsers(request, currentUser);

            log.info("🎉 Bulk creation completed: {} successful, {} failed",
                    response.getResults().getSuccessCount(),
                    response.getResults().getErrorCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error in bulk user creation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BulkCreateUsersResponse.failure("Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Get users with search and filtering
     * GET /api/v1/users?search=&role=&profile=&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<UserListResponse> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String profile,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("📋 Get users request - search: '{}', role: '{}', profile: '{}', page: {}, size: {}",
                    search, role, profile, page, size);

            User currentUser = getCurrentUser(userDetails);
            UserListResponse response = userManagementService.getUsers(search, role, profile, page, size, currentUser);

            if (response.isSuccess()) {
                log.info("✅ Users retrieved successfully: {} users found", response.getUsers().size());
                return ResponseEntity.ok(response);
            } else {
                log.warn("❌ Failed to retrieve users: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("❌ Error getting users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UserListResponse.failure("Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Get user by ID
     * GET /api/v1/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserById(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("👤 Get user by ID request: {}", userId);

            User currentUser = getCurrentUser(userDetails);

            // This would be implemented in the service if needed
            // UserResponse user = userManagementService.getUserById(userId, currentUser);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User details endpoint - to be implemented"
            ));

        } catch (Exception e) {
            log.error("❌ Error getting user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Error retrieving user: " + e.getMessage()
                    ));
        }
    }

    /**
     * Update user
     * PUT /api/v1/users/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<CreateUserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("🔄 Update user request for ID: {}", userId);

            User currentUser = getCurrentUser(userDetails);
            CreateUserResponse response = userManagementService.updateUser(userId, request, currentUser);

            if (response.isSuccess()) {
                log.info("✅ User updated successfully: ID {}", userId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("❌ User update failed: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("❌ Error updating user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CreateUserResponse.failure("Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Delete user (soft delete)
     * DELETE /api/v1/users/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("🗑️ Delete user request for ID: {}", userId);

            User currentUser = getCurrentUser(userDetails);
            boolean deleted = userManagementService.deleteUser(userId, currentUser);

            if (deleted) {
                log.info("✅ User deleted successfully: ID {}", userId);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User deleted successfully"
                ));
            } else {
                log.warn("❌ User deletion failed: ID {}", userId);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to delete user. You may not have permission or user not found."
                ));
            }

        } catch (Exception e) {
            log.error("❌ Error deleting user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Internal server error: " + e.getMessage()
                    ));
        }
    }

    /**
     * Get profiles that current user can create
     * GET /api/v1/users/profiles/allowed
     */
    @GetMapping("/profiles/allowed")
    public ResponseEntity<Map<String, Object>> getAllowedCreationProfiles(
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = getCurrentUser(userDetails);
            List<ProfileResponse> profiles = userManagementService.getAllowedCreationProfiles(currentUser);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "profiles", profiles
            ));

        } catch (Exception e) {
            log.error("❌ Error getting allowed profiles: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Error retrieving profiles: " + e.getMessage()
                    ));
        }
    }

    /**
     * Get roles for a specific profile
     * GET /api/v1/users/roles/for-profile/{profile}
     */
    @GetMapping("/roles/for-profile/{profile}")
    public ResponseEntity<Map<String, Object>> getRolesForProfile(
            @PathVariable String profile) {

        try {
            List<RoleResponse> roles = userManagementService.getAllowedRolesForProfile(profile);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "roles", roles
            ));

        } catch (Exception e) {
            log.error("❌ Error getting roles for profile {}: {}", profile, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Error retrieving roles: " + e.getMessage()
                    ));
        }
    }

    /**
     * Validate email uniqueness
     * GET /api/v1/users/validate-email?email=test@example.com
     */
    @GetMapping("/validate-email")
    public ResponseEntity<Map<String, Object>> validateEmail(
            @RequestParam String email) {

        try {
            boolean available = userManagementService.isEmailAvailable(email);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "available", available,
                "message", available ? "Email is available" : "Email is already in use"
            ));

        } catch (Exception e) {
            log.error("❌ Error validating email {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Error validating email: " + e.getMessage()
                    ));
        }
    }

    /**
     * Validate phone uniqueness
     * GET /api/v1/users/validate-phone?phone=+1234567890
     */
    @GetMapping("/validate-phone")
    public ResponseEntity<Map<String, Object>> validatePhone(
            @RequestParam String phone) {

        try {
            boolean available = userManagementService.isPhoneAvailable(phone);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "available", available,
                "message", available ? "Phone is available" : "Phone is already in use"
            ));

        } catch (Exception e) {
            log.error("❌ Error validating phone {}: {}", phone, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Error validating phone: " + e.getMessage()
                    ));
        }
    }

    /**
     * Helper method to get current user from UserDetails
     */
    private User getCurrentUser(UserDetails userDetails) {
        try {
            log.info("🔍 Getting current user from UserDetails");
            log.info("   👤 UserDetails username: {}", userDetails.getUsername());
            log.info("   🔑 UserDetails authorities: {}", userDetails.getAuthorities());

            String email = userDetails.getUsername();
            log.info("   📧 Looking up user by email: {}", email);

            Optional<User> userOpt = userManagementService.findUserByEmail(email);

            if (userOpt.isEmpty()) {
                log.error("   ❌ USER NOT FOUND in database for email: {}", email);
                log.info("   💡 Checking all users in database...");
                // Let's see what users exist
                throw new IllegalArgumentException("Current user not found in database: " + email);
            }

            User user = userOpt.get();
            log.info("   ✅ Current user found: ID={}, Email={}, Profile={}",
                    user.getUserId(), user.getEmail(),
                    user.getPrimaryProfile() != null ? user.getPrimaryProfile().getName() : "null");

            return user;

        } catch (Exception e) {
            log.error("❌ Error getting current user: {}", e.getMessage(), e);
            throw new SecurityException("Access denied. You do not have permission to perform this action.");
        }
    }
}
