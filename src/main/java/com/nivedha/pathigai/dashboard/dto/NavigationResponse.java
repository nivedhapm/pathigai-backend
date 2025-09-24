package com.nivedha.pathigai.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationResponse {
    private List<NavigationSection> navigation;
    private List<String> permissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavigationSection {
        private String section;
        private List<NavigationItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavigationItem {
        private String id;
        private String label;
        private String path;
        private String icon;
        private Boolean enabled;
        private List<String> requiredPermissions;
    }
}
