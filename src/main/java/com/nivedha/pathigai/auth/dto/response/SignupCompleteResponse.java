package com.nivedha.pathigai.auth.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupCompleteResponse {
    private Integer userId;
    private Integer companyId;
    private String message;
    private Boolean success;
}

