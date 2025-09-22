package com.nivedha.pathigai.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkUploadResponse {
    private Integer accepted;
    private Integer rejected;
    private List<ErrorDetail> errors;

    @Data
    @Builder
    public static class ErrorDetail {
        private Integer row;
        private String message;
    }
}
