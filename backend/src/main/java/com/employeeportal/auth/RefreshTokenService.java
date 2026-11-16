package com.employeeportal.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.employeeportal.common.TokenHasher;
import com.employeeportal.config.AppProperties;
import com.employeeportal.user.User;
import com.employeeportal.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Opaque refresh tokens, rotated on every use (see docs/decisions/0003-jwt-with-refresh-cookie.md).
 * Presenting an already rotated token again means it may have been stolen, so every session of
 * that user is revoked, except within a short grace window that covers two browser tabs
 * refreshing at the same moment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final AppProperties properties;
    private final Clock clock;

    public record Rotation(User user, String refreshToken) {
    }

    /** Creates a refresh token and returns its plain value; only the hash is stored. */
    @Transactional
    public String issue(User user) {
        String token = TokenHasher.generateToken();
        Instant expiresAt = clock.instant().plus(properties.refreshToken().ttl());
        repository.save(new RefreshToken(user.getId(), TokenHasher.sha256(token), expiresAt));
        return token;
    }

    /** Revokes the presented token and issues a new one for the same user. */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String presentedToken) {
        Instant now = clock.instant();
        RefreshToken current = repository.findByTokenHash(TokenHasher.sha256(presentedToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            boolean withinGrace = now.isBefore(current.getRevokedAt().plus(properties.refreshToken().reuseGrace()));
            if (!withinGrace) {
                log.warn("Refresh token reuse detected for user {}; revoking all sessions", current.getUserId());
                repository.revokeAllForUser(current.getUserId(), now);
            }
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(current.getUserId()).orElseThrow(InvalidRefreshTokenException::new);
        current.revoke(now);
        if (!user.isActive()) {
            throw new InvalidRefreshTokenException();
        }
        return new Rotation(user, issue(user));
    }

    /** Logout: revokes one token. Unknown or already revoked tokens are ignored. */
    @Transactional
    public void revoke(String presentedToken) {
        repository.findByTokenHash(TokenHasher.sha256(presentedToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    /** Signs a user out everywhere (deactivation, password change or reset). */
    @Transactional
    public void revokeAll(UUID userId) {
        repository.revokeAllForUser(userId, clock.instant());
    }
}
