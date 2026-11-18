package com.employeeportal.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code /api/auth/activate} and {@code /api/auth/reset-password}. */
public record SetPasswordRequest(
        @NotBlank @Size(max = 100) String token,
        @NotBlank @Size(min = PasswordRules.MIN_LENGTH, max = PasswordRules.MAX_LENGTH) String password) {
}
