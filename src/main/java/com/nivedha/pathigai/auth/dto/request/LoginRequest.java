package com.nivedha.pathigai.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "reCAPTCHA verification is required")
    private String recaptchaToken;

    private Boolean passwordResetComplete;
}