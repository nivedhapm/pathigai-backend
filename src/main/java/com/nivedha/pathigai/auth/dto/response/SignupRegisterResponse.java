package com.nivedha.pathigai.auth.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRegisterResponse {
    private Integer userId;
    private String maskedEmail;
    private String maskedPhone;
    private String nextStep;
    private String message;
}
