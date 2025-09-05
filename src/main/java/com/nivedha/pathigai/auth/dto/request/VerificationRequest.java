package com.nivedha.pathigai.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {

    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 digits")
    private String otp;

    @NotNull(message = "Verification type is required")
    private VerificationType verificationType;

    @NotNull(message = "Context is required")
    private VerificationContext context;

    public enum VerificationType {
        SMS, EMAIL
    }

    public enum VerificationContext {
        SIGNUP, LOGIN, PASSWORD_RESET
    }
}