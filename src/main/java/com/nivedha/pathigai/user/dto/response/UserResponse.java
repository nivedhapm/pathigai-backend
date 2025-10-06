package com.nivedha.pathigai.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String workLocation;
    private String role;
    private String profile;
    private Boolean isActive;
    private Boolean mustChangePassword;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private String companyName;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
