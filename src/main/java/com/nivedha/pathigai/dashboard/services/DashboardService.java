package com.nivedha.pathigai.dashboard.services;

import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.entities.Profile;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.dashboard.dto.DashboardDataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;

    public DashboardDataResponse getDashboardData(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Profile profile = user.getPrimaryProfile();
        String profileName = profile != null ? profile.getName() : "APPLICANT"; // Fixed: was getProfileName()

        return DashboardDataResponse.builder()
                .profile(profileName)
                .stats(getStatsForProfile(profileName, user))
                .recentActivity(getRecentActivityForProfile(profileName, user))
                .quickActions(getQuickActionsForProfile(profileName, user))
                .build();
    }

    public void extendUserSession(String email) {
        // Implement session extension logic
        // This could update last activity timestamp in session management
        System.out.println("Extending session for user: " + email);
    }

    private Map<String, Object> getStatsForProfile(String profileName, User user) {
        Map<String, Object> stats = new HashMap<>();

        switch (profileName) {
            case "SUPER_ADMIN":
                stats.put("totalUsers", getTotalUsers(user.getCompany().getCompanyId()));
                stats.put("totalCompanies", getTotalCompanies());
                stats.put("activeProfiles", getActiveProfiles());
                stats.put("systemHealth", "Good");
                break;
            case "ADMIN":
                stats.put("companyUsers", getCompanyUsers(user.getCompany().getCompanyId()));
                stats.put("pendingApprovals", getPendingApprovals(user.getCompany().getCompanyId()));
                stats.put("activeRoles", getActiveRoles());
                break;
            case "MANAGEMENT":
                stats.put("teamSize", getTeamSize(user.getCompany().getCompanyId()));
                stats.put("activePlacement", getActivePlacements(user.getCompany().getCompanyId()));
                stats.put("monthlyHires", getMonthlyHires(user.getCompany().getCompanyId()));
                break;
            case "TRAINER":
                stats.put("activeTrainees", getActiveTrainees(user.getCompany().getCompanyId()));
                stats.put("completedTrainings", getCompletedTrainings(user.getUserId()));
                stats.put("upcomingSessions", getUpcomingSessions(user.getUserId()));
                break;
            case "APPLICANT":
                stats.put("applications", getApplications(user.getUserId()));
                stats.put("interviewsScheduled", getInterviewsScheduled(user.getUserId()));
                stats.put("profileCompletion", getProfileCompletion(user.getUserId()));
                break;
            default:
                stats.put("message", "Welcome to your dashboard");
        }

        return stats;
    }

    private List<DashboardDataResponse.ActivityItem> getRecentActivityForProfile(String profileName, User user) {
        List<DashboardDataResponse.ActivityItem> activities = new ArrayList<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Sample activities based on profile
        switch (profileName) {
            case "SUPER_ADMIN":
                activities.add(DashboardDataResponse.ActivityItem.builder()
                        .id("1")
                        .description("New company registered: TechCorp")
                        .timestamp(timestamp)
                        .type("registration")
                        .user("System")
                        .build());
                break;
            case "ADMIN":
                activities.add(DashboardDataResponse.ActivityItem.builder()
                        .id("2")
                        .description("New user created: john.doe@company.com")
                        .timestamp(timestamp)
                        .type("user_creation")
                        .user(user.getFullName())
                        .build());
                break;
            case "APPLICANT":
                activities.add(DashboardDataResponse.ActivityItem.builder()
                        .id("3")
                        .description("Application submitted for Java Developer position")
                        .timestamp(timestamp)
                        .type("application")
                        .user(user.getFullName())
                        .build());
                break;
        }

        return activities;
    }

    private List<DashboardDataResponse.QuickAction> getQuickActionsForProfile(String profileName, User user) {
        List<DashboardDataResponse.QuickAction> actions = new ArrayList<>();

        switch (profileName) {
            case "SUPER_ADMIN":
                actions.add(DashboardDataResponse.QuickAction.builder()
                        .id("create_company")
                        .label("Create Company")
                        .path("/dashboard/companies/create")
                        .icon("building")
                        .enabled(true)
                        .build());
                actions.add(DashboardDataResponse.QuickAction.builder()
                        .id("manage_profiles")
                        .label("Manage Profiles")
                        .path("/dashboard/profiles")
                        .icon("users")
                        .enabled(true)
                        .build());
                break;
            case "ADMIN":
                actions.add(DashboardDataResponse.QuickAction.builder()
                        .id("create_user")
                        .label("Create User")
                        .path("/dashboard/users/create")
                        .icon("user-plus")
                        .enabled(true)
                        .build());
                actions.add(DashboardDataResponse.QuickAction.builder()
                        .id("manage_roles")
                        .label("Manage Roles")
                        .path("/dashboard/roles")
                        .icon("shield")
                        .enabled(true)
                        .build());
                break;
            case "APPLICANT":
                actions.add(DashboardDataResponse.QuickAction.builder()
                        .id("apply_job")
                        .label("Apply for Job")
                        .path("/applicant-portal/jobs")
                        .icon("briefcase")
                        .enabled(true)
                        .build());
                actions.add(DashboardDataResponse.QuickAction.builder()
                        .id("update_profile")
                        .label("Update Profile")
                        .path("/applicant-portal/profile")
                        .icon("edit")
                        .enabled(true)
                        .build());
                break;
        }

        return actions;
    }

    // Helper methods for stats calculation
    private int getTotalUsers(Integer companyId) { return 150; } // Implement actual logic
    private int getTotalCompanies() { return 25; }
    private int getActiveProfiles() { return 8; }
    private int getCompanyUsers(Integer companyId) { return 45; }
    private int getPendingApprovals(Integer companyId) { return 5; }
    private int getActiveRoles() { return 12; }
    private int getTeamSize(Integer companyId) { return 30; }
    private int getActivePlacements(Integer companyId) { return 8; }
    private int getMonthlyHires(Integer companyId) { return 12; }
    private int getActiveTrainees(Integer companyId) { return 15; }
    private int getCompletedTrainings(Integer userId) { return 6; }
    private int getUpcomingSessions(Integer userId) { return 3; }
    private int getApplications(Integer userId) { return 4; }
    private int getInterviewsScheduled(Integer userId) { return 2; }
    private int getProfileCompletion(Integer userId) { return 85; }
}
