package com.employeeportal.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update: a {@code null} field is left unchanged. An empty {@code phone} removes the phone number.
 */
public record UpdateUserRequest(
        @Email @Size(max = 255) @Pattern(regexp = UserValidation.NOT_BLANK, message = "must not be blank") String email,
        @Size(max = 150) @Pattern(regexp = UserValidation.NOT_BLANK, message = "must not be blank") String fullName,
        @Size(max = 30) @Pattern(regexp = UserValidation.PHONE_OR_EMPTY, message = UserValidation.PHONE_MESSAGE) String phone,
        Role role) {
}
