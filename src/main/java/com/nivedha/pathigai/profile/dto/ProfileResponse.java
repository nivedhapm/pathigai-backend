package com.nivedha.pathigai.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private Integer profileId;
    private String profileName;
    private String description;
    private Integer hierarchyLevel;
    private Boolean isActive;
}
