package com.nivedha.pathigai.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserResponse {

    private boolean success;
    private String message;
    private Integer userId;
    private UserResponse user;

    public static CreateUserResponse success(Integer userId, UserResponse user) {
        return CreateUserResponse.builder()
                .success(true)
                .message("User created successfully")
                .userId(userId)
                .user(user)
                .build();
    }

    public static CreateUserResponse failure(String message) {
        return CreateUserResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
