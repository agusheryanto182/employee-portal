package com.employeeportal.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 30) @Pattern(regexp = UserValidation.PHONE, message = UserValidation.PHONE_MESSAGE) String phone,
        @NotNull Role role) {
}
