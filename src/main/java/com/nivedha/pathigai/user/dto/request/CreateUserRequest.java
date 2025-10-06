package com.nivedha.pathigai.user.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[\\d\\s\\-()]{10,20}$", message = "Please provide a valid phone number")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    @Pattern(regexp = "^(MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY)$",
             message = "Gender must be one of: MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY")
    private String gender;

    @NotBlank(message = "Work location is required")
    @Size(min = 2, max = 200, message = "Work location must be between 2 and 200 characters")
    private String workLocation;

    @NotBlank(message = "Role is required")
    private String role;

    @NotBlank(message = "Profile is required")
    private String profile;

    @NotBlank(message = "Temporary password is required")
    @Size(min = 8, max = 128, message = "Temporary password must be between 8 and 128 characters")
    private String temporaryPassword;

    @Builder.Default
    private Boolean isActive = true;

    // Validation method for age (minimum 13 years)
    public boolean isValidAge() {
        if (dateOfBirth == null) {
            return false;
        }
        return LocalDate.now().minusYears(13).isAfter(dateOfBirth) ||
               LocalDate.now().minusYears(13).isEqual(dateOfBirth);
    }
}
