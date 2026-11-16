package com.employeeportal.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.employeeportal.common.ClientInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshCookies refreshCookies;
    private final SessionResponses sessionResponses;

    @Operation(summary = "Employee sign in", description = "Employee accounts only (admins use /api/admin/auth/login). Returns an access token and sets the refresh token cookie. Every attempt is recorded in the login history.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthService.Session session = authService.login(request, ClientInfo.from(httpRequest), LoginPortal.EMPLOYEE);
        return sessionResponses.ok(session);
    }

    @Operation(summary = "Get a new access token", description = "Uses and rotates the refresh token cookie.")
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest httpRequest) {
        String refreshToken = refreshCookies.read(httpRequest);
        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new InvalidRefreshTokenException();
            }
            return sessionResponses.ok(authService.refresh(refreshToken));
        } catch (InvalidRefreshTokenException ex) {
            // Also drop the unusable cookie so the browser stops sending it.
            return ResponseEntity.status(ex.getStatus())
                    .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                    .body(ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage()));
        }
    }

    @Operation(summary = "Sign out", description = "Revokes the refresh token and clears the cookie.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        authService.logout(refreshCookies.read(httpRequest));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .build();
    }
}
