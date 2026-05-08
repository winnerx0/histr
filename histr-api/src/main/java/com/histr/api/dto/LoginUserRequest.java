package com.histr.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginUserRequest {

    @NotBlank(message = "Invalid username")
    private String username;

    @NotBlank(message = "Invalid password")
    private String password;
}
