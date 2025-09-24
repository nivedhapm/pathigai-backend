package com.nivedha.pathigai.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDataResponse {
    private String profile;
    private Map<String, Object> stats;
    private List<ActivityItem> recentActivity;
    private List<QuickAction> quickActions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String id;
        private String description;
        private String timestamp;
        private String type;
        private String user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickAction {
        private String id;
        private String label;
        private String path;
        private String icon;
        private Boolean enabled;
    }
}
