package com.nivedha.pathigai.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnhancedAuthResponse {
    private Boolean success;
    private String jwtToken;
    private String refreshToken;
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private Integer userId;
        private String email;
        private String fullName;
        private String role;
        private String profile;
        private String redirectTo;
        private Integer companyId;
        private String companyName;
        private Boolean isTemporaryPassword;
    }
}
