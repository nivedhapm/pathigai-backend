package com.nivedha.pathigai.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private Integer userId;
    private String email;
    private String fullName;
    private String role;
    private String profile;
    private Integer profileLevel;
    private Integer companyId;
    private String companyName;
    private Boolean isCompanyCreator;
    private Boolean isTemporaryPassword;
    private String createdBy;
}
