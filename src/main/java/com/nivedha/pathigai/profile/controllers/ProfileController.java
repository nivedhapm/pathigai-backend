package com.nivedha.pathigai.profile.controllers;

import com.nivedha.pathigai.profile.dto.ProfileResponse;
import com.nivedha.pathigai.profile.dto.ProfileMappingResponse;
import com.nivedha.pathigai.profile.services.ProfileService;
import com.nivedha.pathigai.profile.services.RoleProfileMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final RoleProfileMappingService roleProfileMappingService;

    @GetMapping
    public ResponseEntity<List<ProfileResponse>> getAllProfiles() {
        List<ProfileResponse> profiles = profileService.getAllProfiles();
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/mappings")
    public ResponseEntity<List<ProfileMappingResponse>> getRoleProfileMappings() {
        List<ProfileMappingResponse> mappings = roleProfileMappingService.getAllRoleProfileMappings();
        return ResponseEntity.ok(mappings);
    }

    @GetMapping("/hierarchy")
    public ResponseEntity<List<ProfileResponse>> getProfileHierarchy() {
        List<ProfileResponse> hierarchy = profileService.getProfileHierarchy();
        return ResponseEntity.ok(hierarchy);
    }

    @GetMapping("/allowed-for-role/{roleName}")
    public ResponseEntity<List<ProfileResponse>> getAllowedProfilesForRole(@PathVariable String roleName) {
        List<ProfileResponse> allowedProfiles = roleProfileMappingService.getAllowedProfilesForRole(roleName);
        return ResponseEntity.ok(allowedProfiles);
    }
}
