package com.employeeportal.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Public endpoints behind the links in invite and password-reset emails. */
@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AccountTokenController {

    private final AccountTokenService accountTokenService;

    @Operation(summary = "Check an invite or reset link before showing the password form")
    @GetMapping("/tokens/{token}")
    public AccountTokenInfo inspect(@PathVariable String token) {
        return accountTokenService.inspect(token);
    }

    @Operation(summary = "Set the first password from an invite link and activate the account")
    @PostMapping("/activate")
    public ResponseEntity<Void> activate(@Valid @RequestBody SetPasswordRequest request) {
        accountTokenService.activate(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set a new password from a reset link; signs out every device")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody SetPasswordRequest request) {
        accountTokenService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }
}
