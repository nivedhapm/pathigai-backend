package com.nivedha.pathigai.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private Boolean valid;
    private String status;
    private Long timeUntilExpiration;
    private String profile;
    private String role;
    private String message;
}
