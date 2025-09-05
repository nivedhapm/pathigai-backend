package com.nivedha.pathigai.auth.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationResponse {
    private Boolean verified;
    private String nextStep;
    private String message;
    private Integer remainingAttempts;
}
