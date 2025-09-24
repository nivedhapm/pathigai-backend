package com.nivedha.pathigai.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListResponse {
    private Integer userId;
    private String email;
    private String fullName;
    private String role;
    private String profile;
    private String companyName;
    private Boolean enabled;
    private String userStatus;
}
