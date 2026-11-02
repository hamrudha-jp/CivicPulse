package com.civicpulse.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDtos {

    public record AuthRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record RegisterRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters")
            String password,

            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
            String fullName,

            String phoneNumber
    ) {}

    public record AuthResponse(
            String token,
            String tokenType,
            Long userId,
            String email,
            String fullName,
            String role,
            Long departmentId,
            String departmentName
    ) {}

    public record UserProfileResponse(
            Long id,
            String email,
            String fullName,
            String phoneNumber,
            String role,
            Long departmentId,
            String departmentName
    ) {}
}
