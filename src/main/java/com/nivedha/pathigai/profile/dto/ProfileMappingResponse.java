package com.nivedha.pathigai.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileMappingResponse {
    private String roleName;
    private List<String> allowedProfiles;
    private String creatorProfile;
    private Boolean canCreateRole;
}
