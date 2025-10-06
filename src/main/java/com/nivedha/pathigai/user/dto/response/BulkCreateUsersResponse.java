package com.nivedha.pathigai.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateUsersResponse {

    private boolean success;
    private String message;
    private BulkResults results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkResults {
        private int totalSubmitted;
        private int successCount;
        private int errorCount;
        private List<SuccessfulUser> successfulUsers;
        private List<FailedUser> failedUsers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuccessfulUser {
        private Integer id;
        private String fullName;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedUser {
        private int rowIndex;
        private Object data;
        private List<String> errors;
    }

    public static BulkCreateUsersResponse success(BulkResults results) {
        return BulkCreateUsersResponse.builder()
                .success(true)
                .message("Bulk user creation completed")
                .results(results)
                .build();
    }

    public static BulkCreateUsersResponse failure(String message) {
        return BulkCreateUsersResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
