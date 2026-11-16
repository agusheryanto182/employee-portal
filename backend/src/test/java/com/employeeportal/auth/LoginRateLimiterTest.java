package com.employeeportal.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.employeeportal.support.MutableClock;
import com.employeeportal.support.TestProperties;

class LoginRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final LoginRateLimiter limiter = new LoginRateLimiter(TestProperties.defaults(), clock);
    private final String key = LoginRateLimiter.key("10.0.0.1", "budi@example.com");

    @Test
    void allowsAttemptsBelowTheLimit() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(key);
        }
        assertThat(limiter.retryAfter(key)).isZero();
    }

    @Test
    void blocksAfterMaxFailuresUntilTheOldestFailureLeavesTheWindow() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(key);
            clock.advance(Duration.ofMinutes(1));
        }
        // First failure at 00:00, now 00:05: blocked until 00:15.
        assertThat(limiter.retryAfter(key)).isEqualTo(Duration.ofMinutes(10));

        clock.advance(Duration.ofMinutes(10));
        assertThat(limiter.retryAfter(key)).isZero();
    }

    @Test
    void successfulLoginResetsTheCounter() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(key);
        }
        limiter.reset(key);
        assertThat(limiter.retryAfter(key)).isZero();
    }

    @Test
    void limitsAreSeparatePerIpAndEmail() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(key);
        }
        assertThat(limiter.retryAfter(LoginRateLimiter.key("10.0.0.2", "budi@example.com"))).isZero();
        assertThat(limiter.retryAfter(LoginRateLimiter.key("10.0.0.1", "siti@example.com"))).isZero();
    }
}
