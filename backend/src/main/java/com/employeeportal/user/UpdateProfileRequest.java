package com.employeeportal.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Self-service profile edit. A {@code null} field is left unchanged and an empty {@code phone}
 * removes it. The email is not part of this request: only an admin can change it.
 */
public record UpdateProfileRequest(
        @Size(max = 150) @Pattern(regexp = UserValidation.NOT_BLANK, message = "must not be blank") String fullName,
        @Size(max = 30) @Pattern(regexp = UserValidation.PHONE_OR_EMPTY, message = UserValidation.PHONE_MESSAGE) String phone) {
}
