package com.employeeportal.auth;

import java.time.Duration;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.employeeportal.common.ClientInfo;
import com.employeeportal.loginhistory.LoginFailureReason;
import com.employeeportal.loginhistory.LoginHistoryService;
import com.employeeportal.user.User;
import com.employeeportal.user.UserRepository;
import com.employeeportal.user.UserStatus;

/**
 * Sign in, refresh and sign out. Every login attempt is recorded in the login history
 * (docs/decisions/0005-login-history-includes-failures.md).
 *
 * <p>Not transactional itself: each step (history row, refresh token) commits on its own, so a
 * failed login still leaves its history row.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final LoginHistoryService loginHistoryService;
    private final LoginRateLimiter rateLimiter;

    /**
     * Checked against when the email is unknown or the account has no password yet, so those
     * responses take as long as a real password check and timing does not reveal which emails exist.
     */
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService,
            RefreshTokenService refreshTokenService,
            LoginHistoryService loginHistoryService,
            LoginRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.loginHistoryService = loginHistoryService;
        this.rateLimiter = rateLimiter;
        this.dummyPasswordHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
    }

    /** Result of a login or refresh: the access token for the body and the refresh token for the cookie. */
    public record Session(String accessToken, long expiresInSeconds, String refreshToken, User user) {
    }

    public Session login(LoginRequest request, ClientInfo client) {
        String email = User.normalizeEmail(request.email());
        String limitKey = LoginRateLimiter.key(client.ipAddress(), email);
        User user = userRepository.findByEmail(email).orElse(null);

        Duration retryAfter = rateLimiter.retryAfter(limitKey);
        if (!retryAfter.isZero()) {
            loginHistoryService.recordFailure(user, email, LoginFailureReason.RATE_LIMITED, client);
            throw new TooManyLoginAttemptsException(retryAfter);
        }

        LoginFailureReason failure = checkCredentials(user, request.password());
        if (failure != null) {
            rateLimiter.recordFailure(limitKey);
            loginHistoryService.recordFailure(user, email, failure, client);
            throw new InvalidCredentialsException();
        }

        rateLimiter.reset(limitKey);
        Session session = createSession(user, refreshTokenService.issue(user));
        loginHistoryService.recordSuccess(user, client);
        return session;
    }

    public Session refresh(String refreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        return createSession(rotation.user(), rotation.refreshToken());
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    /** @return null when the credentials are valid and the account may sign in */
    private LoginFailureReason checkCredentials(User user, String password) {
        if (user == null || user.getPasswordHash() == null) {
            passwordEncoder.matches(password, dummyPasswordHash);
            return user == null ? LoginFailureReason.INVALID_CREDENTIALS : LoginFailureReason.ACCOUNT_PENDING;
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return LoginFailureReason.INVALID_CREDENTIALS;
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            return LoginFailureReason.ACCOUNT_INACTIVE;
        }
        if (user.getStatus() == UserStatus.PENDING) {
            return LoginFailureReason.ACCOUNT_PENDING;
        }
        return null;
    }

    private Session createSession(User user, String refreshToken) {
        AccessTokenService.AccessToken accessToken = accessTokenService.issue(user);
        return new Session(accessToken.value(), accessToken.expiresInSeconds(), refreshToken, user);
    }
}
