package com.nivedha.pathigai.user.controllers;

import com.nivedha.pathigai.user.dto.request.CreateUserRequest;
import com.nivedha.pathigai.user.dto.request.UpdateUserRequest;
import com.nivedha.pathigai.user.dto.response.UserProfileResponse;
import com.nivedha.pathigai.user.dto.response.UserListResponse;
import com.nivedha.pathigai.user.services.UserService;
import com.nivedha.pathigai.user.services.UserCreationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserCreationService userCreationService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication) {
        UserProfileResponse profile = userService.getUserProfile(authentication.getName());
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/create")
    public ResponseEntity<UserProfileResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        UserProfileResponse createdUser = userCreationService.createUser(request, authentication.getName());
        return ResponseEntity.ok(createdUser);
    }

    @GetMapping
    public ResponseEntity<List<UserListResponse>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String profile,
            @RequestParam(required = false) Integer companyId,
            Authentication authentication) {
        List<UserListResponse> users = userService.getUsers(role, profile, companyId, authentication.getName());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserById(
            @PathVariable Integer userId,
            Authentication authentication) {
        UserProfileResponse user = userService.getUserById(userId, authentication.getName());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> updateUser(
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        UserProfileResponse updatedUser = userService.updateUser(userId, request, authentication.getName());
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Integer userId,
            Authentication authentication) {
        userService.deleteUser(userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
