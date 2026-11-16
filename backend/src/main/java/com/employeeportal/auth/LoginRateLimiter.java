package com.employeeportal.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.employeeportal.config.AppProperties;

/**
 * Sliding-window limit on failed logins per (IP, email): after {@code maxFailures} failures within
 * {@code window}, further attempts are rejected until the oldest failure leaves the window.
 * A successful login clears the counter.
 *
 * <p>Kept in memory, which is enough for a single instance. Several instances would need a shared
 * store such as Redis.
 */
@Component
public class LoginRateLimiter {

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration window;
    private final Clock clock;

    public LoginRateLimiter(AppProperties properties, Clock clock) {
        this.maxFailures = properties.loginRateLimit().maxFailures();
        this.window = properties.loginRateLimit().window();
        this.clock = clock;
    }

    public static String key(String ipAddress, String email) {
        return ipAddress + "|" + email;
    }

    /** @return how long the caller must wait, or {@link Duration#ZERO} if allowed */
    public Duration retryAfter(String key) {
        Instant now = clock.instant();
        Deque<Instant> attempts = failures.get(key);
        if (attempts == null) {
            return Duration.ZERO;
        }
        synchronized (attempts) {
            prune(attempts, now);
            if (attempts.isEmpty()) {
                failures.remove(key, attempts); // keep the map from growing with stale keys
                return Duration.ZERO;
            }
            if (attempts.size() < maxFailures) {
                return Duration.ZERO;
            }
            return Duration.between(now, attempts.peekFirst().plus(window));
        }
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        Deque<Instant> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts, now);
            attempts.addLast(now);
        }
    }

    public void reset(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Instant> attempts, Instant now) {
        Instant cutoff = now.minus(window);
        while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(cutoff)) {
            attempts.removeFirst();
        }
    }
}
