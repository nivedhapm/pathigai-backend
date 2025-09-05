package com.nivedha.pathigai.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeVerificationTypeRequest {

    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotNull(message = "New verification type is required")
    private NewVerificationType newVerificationType;

    @NotNull(message = "Context is required")
    private VerificationContext context;

    public enum NewVerificationType {
        SMS, EMAIL
    }

    public enum VerificationContext {
        SIGNUP, LOGIN, PASSWORD_RESET
    }
}