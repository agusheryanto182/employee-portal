package com.employeeportal.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.employeeportal.user.UserResponse;

import lombok.RequiredArgsConstructor;

/** Builds the response for a new session: access token in the body, refresh token in the cookie. */
@Component
@RequiredArgsConstructor
public class SessionResponses {

    private final RefreshCookies refreshCookies;

    public ResponseEntity<AuthResponse> ok(AuthService.Session session) {
        AuthResponse body = new AuthResponse(
                session.accessToken(),
                "Bearer",
                session.expiresInSeconds(),
                UserResponse.from(session.user()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.create(session.refreshToken()).toString())
                .body(body);
    }
}
