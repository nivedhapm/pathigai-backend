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
public class UserListResponse {

    private boolean success;
    private String message;
    private List<UserResponse> users;
    private long totalCount;
    private int page;
    private int size;

    public static UserListResponse success(List<UserResponse> users, long totalCount, int page, int size) {
        return UserListResponse.builder()
                .success(true)
                .message("Users retrieved successfully")
                .users(users)
                .totalCount(totalCount)
                .page(page)
                .size(size)
                .build();
    }

    public static UserListResponse failure(String message) {
        return UserListResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
