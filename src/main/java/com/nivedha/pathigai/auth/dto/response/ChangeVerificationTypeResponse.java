package com.nivedha.pathigai.auth.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeVerificationTypeResponse {
    private String maskedContact;
    private String verificationType;
    private String message;
}