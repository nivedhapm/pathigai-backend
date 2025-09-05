package com.nivedha.pathigai.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupCompleteRequest {

    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName;

    @NotBlank(message = "Industry is required")
    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @Size(max = 150, message = "Company website must not exceed 150 characters")
    @Pattern(
            regexp = "^$|^https?://.*",
            message = "Company website must be a valid URL starting with http:// or https://"
    )
    private String companyWebsite;
}