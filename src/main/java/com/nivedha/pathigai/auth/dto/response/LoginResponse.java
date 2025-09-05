package com.nivedha.pathigai.auth.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private Integer userId;
    private String email;
    private String phone;
    private String fullName;
    private String maskedEmail;
    private String maskedPhone;
    private String message;
    private String nextStep;
    private Boolean isTemporaryPassword;
    private String jwtToken;
    private String refreshToken;
    private Long tokenExpiresIn;
}