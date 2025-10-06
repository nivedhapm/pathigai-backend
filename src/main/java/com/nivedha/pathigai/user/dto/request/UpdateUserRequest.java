package com.nivedha.pathigai.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String role;
    private String profile;
    private Boolean enabled;
    private Boolean isTemporaryPassword;
}
