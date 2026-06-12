package com.booknest.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO for user registration requests
@Data
public class RegisterRequest {

    // User's full name (required)
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 50, message = "Full name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\-']+(?: [a-zA-Z\\-']+)*$", message = "Full name can only contain letters, single spaces, hyphens and apostrophes, and cannot start or end with a space")
    private String fullName;

    // User's email address (required, must be valid)
    @Email(message = "Please provide a valid email")
    @NotBlank(message = "Email is required")
    private String email;

    // User's password (required, min 8 chars, must contain letter and number)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one number")
    @lombok.ToString.Exclude
    private String password;

    // User's mobile number (optional, must be 10 digits if provided)
    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobile;
}