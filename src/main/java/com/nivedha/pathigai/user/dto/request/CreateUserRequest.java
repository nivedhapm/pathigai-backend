package com.nivedha.pathigai.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[\\d\\s\\-()]{10,}$", message = "Invalid phone format")
    private String phone;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Role is required")
    private String role;

    @NotBlank(message = "Profile is required")
    private String profile;

    @NotBlank(message = "Temporary password is required")
    @Size(min = 8, message = "Temporary password must be at least 8 characters")
    private String temporaryPassword;

    @Builder.Default
    private boolean isActive = true;

    private String gender;
    private String workLocation;
}
