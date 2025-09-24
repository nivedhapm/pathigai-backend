package com.nivedha.pathigai.dashboard.controllers;

import com.nivedha.pathigai.dashboard.dto.DashboardDataResponse;
import com.nivedha.pathigai.dashboard.dto.NavigationResponse;
import com.nivedha.pathigai.dashboard.services.DashboardService;
import com.nivedha.pathigai.dashboard.services.NavigationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final NavigationService navigationService;

    @GetMapping("/data")
    public ResponseEntity<DashboardDataResponse> getDashboardData(Authentication authentication) {
        DashboardDataResponse dashboardData = dashboardService.getDashboardData(authentication.getName());
        return ResponseEntity.ok(dashboardData);
    }

    @GetMapping("/navigation")
    public ResponseEntity<NavigationResponse> getNavigation(Authentication authentication) {
        NavigationResponse navigation = navigationService.getNavigation(authentication.getName());
        return ResponseEntity.ok(navigation);
    }

    @PostMapping("/extend-session")
    public ResponseEntity<Void> extendSession(Authentication authentication) {
        dashboardService.extendUserSession(authentication.getName());
        return ResponseEntity.ok().build();
    }
}
