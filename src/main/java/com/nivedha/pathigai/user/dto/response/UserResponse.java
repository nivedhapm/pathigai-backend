package com.nivedha.pathigai.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String profile;
    private boolean isActive;
    private boolean mustChangePassword;
    private String companyName;
    private String createdByName;
    private LocalDateTime createdAt;
}
