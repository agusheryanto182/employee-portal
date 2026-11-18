package com.employeeportal.auth;

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

/** Backoffice sign-in. Refresh and logout are shared with employees under /api/auth. */
@Tag(name = "Auth")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final SessionResponses sessionResponses;

    @Operation(summary = "Admin sign in", description = "Admin accounts only. Same response and cookie as the employee sign in.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthService.Session session = authService.login(request, ClientInfo.from(httpRequest), LoginPortal.ADMIN);
        return sessionResponses.ok(session);
    }
}
