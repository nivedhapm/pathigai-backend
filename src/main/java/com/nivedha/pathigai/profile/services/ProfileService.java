package com.nivedha.pathigai.profile.services;

import com.nivedha.pathigai.auth.entities.Profile;
import com.nivedha.pathigai.profile.dto.ProfileResponse;
import com.nivedha.pathigai.auth.repositories.ProfileRepository; // Changed: use auth module repository
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    public List<ProfileResponse> getAllProfiles() {
        return profileRepository.findAll() // Changed from findAllByIsActiveTrue() since isActive doesn't exist
                .stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    public List<ProfileResponse> getProfileHierarchy() {
        return profileRepository.findAllByOrderByHierarchyLevelAsc() // Updated method name
                .stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    public ProfileResponse getProfileById(Integer profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found with id: " + profileId));
        return mapToProfileResponse(profile);
    }

    public ProfileResponse getProfileByName(String profileName) {
        Profile profile = profileRepository.findByName(profileName) // Updated method name
                .orElseThrow(() -> new RuntimeException("Profile not found: " + profileName));
        return mapToProfileResponse(profile);
    }

    private ProfileResponse mapToProfileResponse(Profile profile) {
        return ProfileResponse.builder()
                .profileId(profile.getProfileId())
                .profileName(profile.getName()) // Changed from getProfileName() to getName()
                .description(profile.getDescription())
                .hierarchyLevel(profile.getHierarchyLevel())
                .isActive(true) // Profile entity doesn't have isActive field, defaulting to true
                .build();
    }
}
