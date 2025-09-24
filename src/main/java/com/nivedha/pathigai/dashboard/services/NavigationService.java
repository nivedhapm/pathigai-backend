package com.nivedha.pathigai.dashboard.services;

import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.entities.Profile;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.dashboard.dto.NavigationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class NavigationService {

    private final UserRepository userRepository;

    public NavigationResponse getNavigation(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Profile profile = user.getPrimaryProfile();
        String profileName = profile != null ? profile.getName() : "APPLICANT"; // Fixed: was getProfileName()

        return NavigationResponse.builder()
                .navigation(getNavigationForProfile(profileName))
                .permissions(getPermissionsForProfile(profileName))
                .build();
    }

    private List<NavigationResponse.NavigationSection> getNavigationForProfile(String profileName) {
        List<NavigationResponse.NavigationSection> sections = new ArrayList<>();

        switch (profileName) {
            case "SUPER_ADMIN":
                sections.add(createAdministrationSection());
                sections.add(createCompanyManagementSection());
                sections.add(createSystemSection());
                sections.add(createReportsSection());
                break;
            case "ADMIN":
                sections.add(createUserManagementSection());
                sections.add(createRoleManagementSection());
                sections.add(createCompanySection());
                sections.add(createReportsSection());
                break;
            case "MANAGEMENT":
                sections.add(createPlacementSection());
                sections.add(createTeamSection());
                sections.add(createAnalyticsSection());
                break;
            case "TRAINER":
                sections.add(createTrainingSection());
                sections.add(createTraineeSection());
                break;
            case "INTERVIEW_PANELIST":
                sections.add(createInterviewSection());
                sections.add(createCandidateSection());
                break;
            case "PLACEMENT":
                sections.add(createJobSection());
                sections.add(createCandidateSection());
                break;
            case "TRAINEE":
                sections.add(createLearningSection());
                sections.add(createAssignmentSection());
                break;
            case "APPLICANT":
                sections.add(createJobSearchSection());
                sections.add(createApplicationSection());
                sections.add(createProfileSection());
                break;
        }

        return sections;
    }

    private List<String> getPermissionsForProfile(String profileName) {
        List<String> permissions = new ArrayList<>();

        switch (profileName) {
            case "SUPER_ADMIN":
                permissions.addAll(Arrays.asList(
                    "user.create", "user.view", "user.edit", "user.delete",
                    "company.create", "company.view", "company.edit", "company.delete",
                    "profile.create", "profile.view", "profile.edit", "profile.delete",
                    "role.create", "role.view", "role.edit", "role.delete",
                    "system.view", "system.edit", "reports.view"
                ));
                break;
            case "ADMIN":
                permissions.addAll(Arrays.asList(
                    "user.create", "user.view", "user.edit",
                    "role.create", "role.view", "role.edit",
                    "company.view", "company.edit",
                    "reports.view"
                ));
                break;
            case "MANAGEMENT":
                permissions.addAll(Arrays.asList(
                    "placement.view", "placement.edit",
                    "team.view", "team.edit",
                    "analytics.view", "reports.view"
                ));
                break;
            case "TRAINER":
                permissions.addAll(Arrays.asList(
                    "training.create", "training.view", "training.edit",
                    "trainee.view", "trainee.edit"
                ));
                break;
            case "APPLICANT":
                permissions.addAll(Arrays.asList(
                    "job.view", "application.create", "application.view",
                    "profile.view", "profile.edit"
                ));
                break;
        }

        return permissions;
    }

    // Navigation section builders
    private NavigationResponse.NavigationSection createAdministrationSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Administration")
                .items(Arrays.asList(
                    createNavigationItem("users", "User Management", "/dashboard/users", "users"),
                    createNavigationItem("profiles", "Profile Management", "/dashboard/profiles", "shield"),
                    createNavigationItem("companies", "Company Management", "/dashboard/companies", "building")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createUserManagementSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("User Management")
                .items(Arrays.asList(
                    createNavigationItem("users", "Users", "/dashboard/users", "users"),
                    createNavigationItem("create_user", "Create User", "/dashboard/users/create", "user-plus"),
                    createNavigationItem("roles", "Roles", "/dashboard/roles", "shield")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createJobSearchSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Job Search")
                .items(Arrays.asList(
                    createNavigationItem("jobs", "Available Jobs", "/applicant-portal/jobs", "briefcase"),
                    createNavigationItem("applications", "My Applications", "/applicant-portal/applications", "file-text"),
                    createNavigationItem("interviews", "Interviews", "/applicant-portal/interviews", "calendar")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createCompanyManagementSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Company Management")
                .items(Arrays.asList(
                    createNavigationItem("companies", "Companies", "/dashboard/companies", "building"),
                    createNavigationItem("create_company", "Create Company", "/dashboard/companies/create", "plus")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createSystemSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("System")
                .items(Arrays.asList(
                    createNavigationItem("system", "System Settings", "/dashboard/system", "settings"),
                    createNavigationItem("logs", "System Logs", "/dashboard/logs", "file-text")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createReportsSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Reports")
                .items(Arrays.asList(
                    createNavigationItem("reports", "Reports", "/dashboard/reports", "bar-chart"),
                    createNavigationItem("analytics", "Analytics", "/dashboard/analytics", "trending-up")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createRoleManagementSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Role Management")
                .items(Arrays.asList(
                    createNavigationItem("roles", "Roles", "/dashboard/roles", "shield"),
                    createNavigationItem("permissions", "Permissions", "/dashboard/permissions", "key")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createCompanySection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Company")
                .items(Arrays.asList(
                    createNavigationItem("company_info", "Company Info", "/dashboard/company", "building"),
                    createNavigationItem("company_settings", "Settings", "/dashboard/company/settings", "settings")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createPlacementSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Placement")
                .items(Arrays.asList(
                    createNavigationItem("placements", "Placements", "/dashboard/placements", "briefcase"),
                    createNavigationItem("job_postings", "Job Postings", "/dashboard/jobs", "file-plus")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createTeamSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Team")
                .items(Arrays.asList(
                    createNavigationItem("team", "Team Members", "/dashboard/team", "users"),
                    createNavigationItem("performance", "Performance", "/dashboard/performance", "trending-up")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createAnalyticsSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Analytics")
                .items(Arrays.asList(
                    createNavigationItem("analytics", "Analytics", "/dashboard/analytics", "bar-chart"),
                    createNavigationItem("kpi", "KPI Dashboard", "/dashboard/kpi", "target")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createTrainingSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Training")
                .items(Arrays.asList(
                    createNavigationItem("trainings", "Training Programs", "/dashboard/trainings", "book"),
                    createNavigationItem("create_training", "Create Training", "/dashboard/trainings/create", "plus")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createTraineeSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Trainees")
                .items(Arrays.asList(
                    createNavigationItem("trainees", "My Trainees", "/dashboard/trainees", "users"),
                    createNavigationItem("progress", "Progress Tracking", "/dashboard/progress", "trending-up")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createInterviewSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Interviews")
                .items(Arrays.asList(
                    createNavigationItem("interviews", "Scheduled Interviews", "/dashboard/interviews", "calendar"),
                    createNavigationItem("feedback", "Interview Feedback", "/dashboard/feedback", "message-square")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createCandidateSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Candidates")
                .items(Arrays.asList(
                    createNavigationItem("candidates", "Candidates", "/dashboard/candidates", "users"),
                    createNavigationItem("evaluations", "Evaluations", "/dashboard/evaluations", "check-square")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createJobSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Jobs")
                .items(Arrays.asList(
                    createNavigationItem("jobs", "Job Openings", "/dashboard/jobs", "briefcase"),
                    createNavigationItem("applications", "Applications", "/dashboard/applications", "file-text")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createLearningSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Learning")
                .items(Arrays.asList(
                    createNavigationItem("courses", "My Courses", "/dashboard/courses", "book"),
                    createNavigationItem("progress", "Learning Progress", "/dashboard/learning-progress", "trending-up")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createAssignmentSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Assignments")
                .items(Arrays.asList(
                    createNavigationItem("assignments", "Assignments", "/dashboard/assignments", "clipboard"),
                    createNavigationItem("submissions", "Submissions", "/dashboard/submissions", "upload")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createApplicationSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Applications")
                .items(Arrays.asList(
                    createNavigationItem("applications", "My Applications", "/applicant-portal/applications", "file-text"),
                    createNavigationItem("status", "Application Status", "/applicant-portal/status", "clock")
                ))
                .build();
    }

    private NavigationResponse.NavigationSection createProfileSection() {
        return NavigationResponse.NavigationSection.builder()
                .section("Profile")
                .items(Arrays.asList(
                    createNavigationItem("profile", "My Profile", "/applicant-portal/profile", "user"),
                    createNavigationItem("documents", "Documents", "/applicant-portal/documents", "file")
                ))
                .build();
    }

    private NavigationResponse.NavigationItem createNavigationItem(String id, String label, String path, String icon) {
        return NavigationResponse.NavigationItem.builder()
                .id(id)
                .label(label)
                .path(path)
                .icon(icon)
                .enabled(true)
                .requiredPermissions(Arrays.asList(id + ".view"))
                .build();
    }
}
