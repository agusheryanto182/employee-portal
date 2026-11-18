package com.employeeportal.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Application settings under the {@code app.*} prefix. The app refuses to start if they are invalid. */
@Validated
@ConfigurationProperties("app")
public record AppProperties(
        @NotBlank String baseUrl,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull RefreshToken refreshToken,
        @Valid @NotNull LoginRateLimit loginRateLimit,
        @Valid @NotNull AccountToken accountToken,
        @Valid @NotNull Mail mail,
        @Valid @NotNull Admin admin) {

    /**
     * @param secret HMAC key for signing access tokens; at least 32 bytes for HS256
     */
    public record Jwt(
            @NotBlank @Size(min = 32, message = "must be at least 32 characters") String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl) {
    }

    /**
     * @param reuseGrace a rotated token presented again within this window (for example by a second
     *                   browser tab racing the first) is rejected without revoking every session
     */
    public record RefreshToken(
            @NotNull Duration ttl,
            @NotBlank String cookieName,
            boolean cookieSecure,
            @NotNull Duration reuseGrace) {
    }

    public record LoginRateLimit(@Min(1) int maxFailures, @NotNull Duration window) {
    }

    /** Lifetime of one-time links sent by email (docs/decisions/0004-invite-link-onboarding.md). */
    public record AccountToken(@NotNull Duration inviteTtl, @NotNull Duration resetTtl) {
    }

    /** @param from sender of every email, for example {@code Employee Portal <no-reply@example.com>} */
    public record Mail(@NotBlank String from) {
    }

    /** First admin, created on startup when the email is set and no such account exists. */
    public record Admin(String email, String password, String fullName) {

        public boolean isConfigured() {
            return email != null && !email.isBlank() && password != null && !password.isBlank();
        }
    }
}
