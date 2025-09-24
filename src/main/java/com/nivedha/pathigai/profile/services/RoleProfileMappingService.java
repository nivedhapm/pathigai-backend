package com.nivedha.pathigai.profile.services;

import com.nivedha.pathigai.profile.dto.ProfileMappingResponse;
import com.nivedha.pathigai.profile.dto.ProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RoleProfileMappingService {

    private final ProfileService profileService;

    // Role-Profile mapping logic as per your requirements
    private static final Map<String, Map<String, Object>> ROLE_PROFILE_MAPPINGS = createRoleProfileMappings();

    private static Map<String, Map<String, Object>> createRoleProfileMappings() {
        Map<String, Map<String, Object>> mappings = new HashMap<>();

        // SUPER_ADMIN mappings
        Map<String, Object> superAdminMappings = new HashMap<>();
        superAdminMappings.put("ADMIN", "SUPER_ADMIN");
        superAdminMappings.put("MANAGER", Arrays.asList("SUPER_ADMIN", "ADMIN", "MANAGEMENT"));
        superAdminMappings.put("HR", Arrays.asList("SUPER_ADMIN", "ADMIN", "MANAGEMENT"));
        superAdminMappings.put("FACULTY", Arrays.asList("ADMIN", "TRAINER"));
        superAdminMappings.put("MENTOR", "TRAINER");
        superAdminMappings.put("INTERVIEW_PANELIST", "INTERVIEW_PANELIST");
        superAdminMappings.put("EMPLOYEE", "PLACEMENT");
        superAdminMappings.put("TRAINEE", "TRAINEE");
        superAdminMappings.put("APPLICANT", null); // null is allowed in HashMap
        mappings.put("SUPER_ADMIN", superAdminMappings);

        // ADMIN mappings
        Map<String, Object> adminMappings = new HashMap<>();
        adminMappings.put("MANAGER", Arrays.asList("ADMIN", "MANAGEMENT"));
        adminMappings.put("HR", Arrays.asList("ADMIN", "MANAGEMENT"));
        adminMappings.put("FACULTY", "TRAINER");
        adminMappings.put("MENTOR", "TRAINER");
        adminMappings.put("INTERVIEW_PANELIST", "INTERVIEW_PANELIST");
        adminMappings.put("EMPLOYEE", "PLACEMENT");
        adminMappings.put("TRAINEE", "TRAINEE");
        adminMappings.put("APPLICANT", null);
        mappings.put("ADMIN", adminMappings);

        // MANAGEMENT mappings
        Map<String, Object> managementMappings = new HashMap<>();
        managementMappings.put("FACULTY", "TRAINER");
        managementMappings.put("MENTOR", "TRAINER");
        managementMappings.put("EMPLOYEE", "PLACEMENT");
        managementMappings.put("TRAINEE", "TRAINEE");
        managementMappings.put("APPLICANT", null);
        mappings.put("MANAGEMENT", managementMappings);

        // TRAINER mappings
        Map<String, Object> trainerMappings = new HashMap<>();
        trainerMappings.put("TRAINEE", "TRAINEE");
        trainerMappings.put("APPLICANT", null);
        mappings.put("TRAINER", trainerMappings);

        return mappings;
    }

    public boolean validateRoleProfileMapping(String creatorProfile, String targetRole, String targetProfile) {
        if (!ROLE_PROFILE_MAPPINGS.containsKey(creatorProfile)) {
            return false;
        }

        Map<String, Object> allowedMappings = ROLE_PROFILE_MAPPINGS.get(creatorProfile);

        if (!allowedMappings.containsKey(targetRole)) {
            return false;
        }

        Object allowedProfiles = allowedMappings.get(targetRole);

        // Special case for APPLICANT role
        if ("APPLICANT".equals(targetRole)) {
            return targetProfile == null;
        }

        if (allowedProfiles == null) {
            return targetProfile == null;
        }

        if (allowedProfiles instanceof List) {
            return ((List<?>) allowedProfiles).contains(targetProfile);
        }

        return allowedProfiles.equals(targetProfile);
    }

    public List<ProfileResponse> getAllowedProfilesForRole(String roleName) {
        // This would typically query based on the current user's profile
        // For now, returning all profiles - implement based on current user context
        return profileService.getAllProfiles();
    }

    public List<ProfileMappingResponse> getAllRoleProfileMappings() {
        List<ProfileMappingResponse> mappings = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> creatorEntry : ROLE_PROFILE_MAPPINGS.entrySet()) {
            String creatorProfile = creatorEntry.getKey();

            for (Map.Entry<String, Object> roleEntry : creatorEntry.getValue().entrySet()) {
                String role = roleEntry.getKey();
                Object allowedProfiles = roleEntry.getValue();

                List<String> profileList = new ArrayList<>();
                if (allowedProfiles instanceof List) {
                    profileList = (List<String>) allowedProfiles;
                } else if (allowedProfiles instanceof String) {
                    profileList.add((String) allowedProfiles);
                }

                mappings.add(ProfileMappingResponse.builder()
                    .roleName(role)
                    .allowedProfiles(profileList)
                    .creatorProfile(creatorProfile)
                    .canCreateRole(true)
                    .build());
            }
        }

        return mappings;
    }
}
