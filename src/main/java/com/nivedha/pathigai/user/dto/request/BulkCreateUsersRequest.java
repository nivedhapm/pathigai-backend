package com.nivedha.pathigai.user.dto.request;

import com.nivedha.pathigai.user.dto.request.CreateUserRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateUsersRequest {

    @NotEmpty(message = "Users list cannot be empty")
    @Valid
    private List<CreateUserRequest> users;
}
