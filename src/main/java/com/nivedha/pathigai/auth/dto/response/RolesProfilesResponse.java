package com.nivedha.pathigai.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RolesProfilesResponse {
    private java.util.List<RoleDto> roles;
    private java.util.List<ProfileDto> profiles;

    @Data
    @Builder
    public static class RoleDto {
        private Integer roleId;
        private String name;
    }

    @Data
    @Builder
    public static class ProfileDto {
        private Integer profileId;
        private String name;
        private String description;
        private Integer hierarchyLevel;
    }
}
