package com.nivedha.pathigai.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Integer userId;
    private String email;
    private String fullName;
    private String role;
    private String profile; // Can be null for APPLICANT
    private Integer profileLevel;
    private Integer companyId;
    private String companyName;
    private Boolean isCompanyCreator;
    private Boolean isTemporaryPassword;
    private String createdBy;
    private Boolean enabled;
    private String userStatus;
}
