package com.histr.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class RegisterUserRequest {

    @NotBlank(message = "Invalid username")
    private String username;

    @NotBlank(message = "Invalid username")
    @Email(message = "Invalid email")
    private String email;

    @NotBlank(message = "Invalid password")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
