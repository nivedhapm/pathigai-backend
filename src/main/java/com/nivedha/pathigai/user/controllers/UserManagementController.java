package com.nivedha.pathigai.user.controllers;

import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.user.dto.request.CreateUserRequest;
import com.nivedha.pathigai.user.dto.request.BulkCreateUsersRequest;
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
     * Get all users in the company
     * GET /api/v1/users
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("📋 Fetching all users for company");

            User currentUser = getCurrentUser(userDetails);
            List<UserResponse> users = userManagementService.getAllUsers(currentUser);

            log.info("✅ Retrieved {} users", users.size());
            return ResponseEntity.ok(users);

        } catch (Exception e) {
            log.error("❌ Error fetching users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    /**
     * Get profiles that current user can create
     * GET /api/v1/profiles/allowed
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
     * GET /api/v1/roles/for-profile/{profile}
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
                "available", available,
                "message", available ? "Email is available" : "Email already exists"
            ));

        } catch (Exception e) {
            log.error("❌ Error validating email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "available", false,
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
                "available", available,
                "message", available ? "Phone number is available" : "Phone number already exists"
            ));

        } catch (Exception e) {
            log.error("❌ Error validating phone: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "available", false,
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
