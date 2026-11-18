package com.employeeportal.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.employeeportal.auth.AuthResponse;
import com.employeeportal.auth.ChangePasswordRequest;
import com.employeeportal.auth.PasswordChangeService;
import com.employeeportal.auth.SessionResponses;
import com.employeeportal.common.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Self-service for any signed-in user: employees from the dashboard, admins from "My account". */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "My account")
@SecurityRequirement(name = "bearer")
public class MeController {

    private final ProfileService profileService;
    private final PasswordChangeService passwordChangeService;
    private final SessionResponses sessionResponses;

    @GetMapping
    @Operation(summary = "My profile")
    public UserResponse get(Authentication authentication) {
        return profileService.get(CurrentUser.id(authentication));
    }

    @PatchMapping
    @Operation(summary = "Edit my full name or phone; the email can only be changed by an admin")
    public UserResponse update(@Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
        return profileService.update(CurrentUser.id(authentication), request);
    }

    @PutMapping("/password")
    @Operation(summary = "Change my password",
            description = "Signs out every other device. Returns a new access token and sets a new refresh token cookie for this one.")
    public ResponseEntity<AuthResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        return sessionResponses.ok(passwordChangeService.change(
                CurrentUser.id(authentication), request.currentPassword(), request.newPassword()));
    }
}
