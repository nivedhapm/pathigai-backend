package com.nivedha.pathigai.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateUsersRequest {

    @NotEmpty(message = "Users list cannot be empty")
    @Size(max = 1000, message = "Cannot process more than 1000 users at once")
    @Valid
    private List<CreateUserRequest> users;
}
