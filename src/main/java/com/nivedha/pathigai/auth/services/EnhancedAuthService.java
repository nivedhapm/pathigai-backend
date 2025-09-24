package com.nivedha.pathigai.auth.services;

import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.entities.Profile;
import com.nivedha.pathigai.auth.entities.Role;
import com.nivedha.pathigai.auth.dto.response.AuthResponse;
import com.nivedha.pathigai.auth.dto.response.SessionResponse;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import com.nivedha.pathigai.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnhancedAuthService {

    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;

    public AuthResponse createAuthResponse(User user, String accessToken, String refreshToken) {
        Profile profile = user.getPrimaryProfile();
        Role role = user.getPrimaryRole();

        String redirectTo = determineRedirectPath(profile, role);

        return AuthResponse.builder()
                .success(true)
                .jwtToken(accessToken)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserInfo.builder()
                        .userId(user.getUserId())
                        .role(role != null ? role.getName() : null)
                        .profile(profile != null ? profile.getName() : null) // Fixed: was getProfileName()
                        .redirectTo(redirectTo)
                        .build())
                .build();
    }

    public SessionResponse validateSession(String token) {
        try {
            if (jwtConfig.isTokenExpired(token)) {
                return SessionResponse.builder()
                        .valid(false)
                        .status("EXPIRED")
                        .message("Session expired")
                        .build();
            }

            String email = jwtConfig.extractUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Profile profile = user.getPrimaryProfile();
            Role role = user.getPrimaryRole();

            return SessionResponse.builder()
                    .valid(true)
                    .status("ACTIVE")
                    .timeUntilExpiration(jwtConfig.getTimeUntilExpiration(token))
                    .profile(profile != null ? profile.getName() : null) // Fixed: was getProfileName()
                    .role(role != null ? role.getName() : null)
                    .build();

        } catch (Exception e) {
            return SessionResponse.builder()
                    .valid(false)
                    .status("INVALID")
                    .message("Invalid session")
                    .build();
        }
    }

    public void extendSession(String email) {
        // Update last activity timestamp
        // This could be implemented with session tracking in database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        // Implement session extension logic here
        // For example, update last_activity timestamp
    }

    private String determineRedirectPath(Profile profile, Role role) {
        if (profile != null) {
            // Users with profiles go to dashboard
            return "/dashboard";
        } else if (role != null && "APPLICANT".equals(role.getName())) {
            // APPLICANT role users go to applicant portal
            return "/applicant-portal";
        } else {
            // Default to dashboard
            return "/dashboard";
        }
    }
}
