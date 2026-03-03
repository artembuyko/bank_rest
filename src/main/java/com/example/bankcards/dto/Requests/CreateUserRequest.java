package com.example.bankcards.dto.Requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Validated
public class CreateUserRequest {

    @NotBlank(message = "username is required")
    @Size(min = 4, max = 50, message = "username must be between 4 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "username can only contain letters, numbers, dots and underscores")
    private String username;

    @NotBlank(message = "password cannot be empty")
    @Size(min = 8, max = 25, message = "password must be between 8 and 25 characters")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message = "password must contain at least one digit, one lowercase, one uppercase letter, and one special character"
    )
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String name;

    private String role;
}
