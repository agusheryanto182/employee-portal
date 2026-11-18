package com.employeeportal.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import com.employeeportal.config.AppProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The refresh token cookie: httpOnly (unreadable by JavaScript), SameSite=Lax (not sent on
 * cross-site POSTs, which blocks CSRF), and scoped to {@code /api/auth} so it is only sent to
 * the auth endpoints.
 */
@Component
public class RefreshCookies {

    static final String PATH = "/api/auth";

    private final AppProperties.RefreshToken settings;

    public RefreshCookies(AppProperties properties) {
        this.settings = properties.refreshToken();
    }

    public ResponseCookie create(String refreshToken) {
        return build(refreshToken, settings.ttl());
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, settings.cookieName());
        return cookie == null ? null : cookie.getValue();
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(settings.cookieName(), value)
                .httpOnly(true)
                .secure(settings.cookieSecure())
                .sameSite("Lax")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
