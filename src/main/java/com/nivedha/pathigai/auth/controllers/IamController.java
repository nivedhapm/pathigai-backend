package com.nivedha.pathigai.auth.controllers;

import com.nivedha.pathigai.auth.dto.request.CreateUserRequest;
import com.nivedha.pathigai.auth.dto.response.RolesProfilesResponse;
import com.nivedha.pathigai.auth.dto.response.UserProfileResponse;
import com.nivedha.pathigai.auth.services.IamService;
import com.nivedha.pathigai.config.JwtConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/iam")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class IamController {

    private final IamService iamService;
    private final JwtConfig jwtConfig;

    @GetMapping("/roles-profiles")
    public ResponseEntity<RolesProfilesResponse> getRolesAndProfiles() {
        log.info("GET /api/v1/iam/roles-profiles - Fetching roles and profiles");

        RolesProfilesResponse response = iamService.getRolesAndProfiles();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        log.info("GET /api/v1/iam/users - query: '{}', page: {}, size: {}", query, page, size);

        // Extract company ID from JWT token
        String token = extractTokenFromRequest(request);
        Integer companyId = jwtConfig.extractCompanyId(token);

        Page<UserProfileResponse> userPage = iamService.getUsers(query, page, size, companyId);

        Map<String, Object> response = new HashMap<>();
        response.put("items", userPage.getContent());
        response.put("total", userPage.getTotalElements());
        response.put("page", userPage.getNumber());
        response.put("size", userPage.getSize());
        response.put("totalPages", userPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/users")
    public ResponseEntity<UserProfileResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/v1/iam/users - Creating user with email: {}", request.getEmail());

        // Extract creator user ID from JWT token
        String token = extractTokenFromRequest(httpRequest);
        Integer createdByUserId = jwtConfig.extractUserId(token);

        UserProfileResponse response = iamService.createUser(request, createdByUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserProfileResponse> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/v1/iam/users/{} - Updating user", id);

        // Extract updater user ID from JWT token
        String token = extractTokenFromRequest(httpRequest);
        Integer updatedByUserId = jwtConfig.extractUserId(token);

        UserProfileResponse response = iamService.updateUser(id, request, updatedByUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "true") boolean soft,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/v1/iam/users/{} - soft: {}", id, soft);

        if (!soft) {
            throw new UnsupportedOperationException("Hard delete not supported for security reasons");
        }

        // Extract deleter user ID from JWT token
        String token = extractTokenFromRequest(httpRequest);
        Integer deletedByUserId = jwtConfig.extractUserId(token);

        iamService.softDeleteUser(id, deletedByUserId);
        return ResponseEntity.noContent().build();
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new IllegalArgumentException("Invalid or missing authorization token");
    }
}
