package com.nivedha.pathigai.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Boolean success;
    private String jwtToken;
    private String refreshToken;
    private UserInfo user;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Integer userId;
        private String role;
        private String profile; // Can be null for APPLICANT
        private String redirectTo; // "/dashboard" or "/applicant-portal"
    }
}
