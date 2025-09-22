package com.nivedha.pathigai.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {

    private String authToken;  // New JWT access token
    private String refreshToken;  // New refresh token (optional rotation)
    private String message;
    private Long expiresIn;  // Token expiration in milliseconds
}
