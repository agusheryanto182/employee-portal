package com.employeeportal.support;

import java.time.Duration;

import com.employeeportal.config.AppProperties;

public final class TestProperties {

    private TestProperties() {
    }

    public static AppProperties defaults() {
        return new AppProperties(
                "http://localhost:5173",
                new AppProperties.Jwt("unit-test-secret-0123456789-0123456789", "employee-portal", Duration.ofMinutes(15)),
                new AppProperties.RefreshToken(Duration.ofDays(7), "refresh_token", true, Duration.ofSeconds(30)),
                new AppProperties.LoginRateLimit(5, Duration.ofMinutes(15)),
                new AppProperties.AccountToken(Duration.ofHours(24), Duration.ofHours(1)),
                new AppProperties.Mail("Employee Portal <no-reply@test.example>"),
                new AppProperties.Admin(null, null, null));
    }
}
