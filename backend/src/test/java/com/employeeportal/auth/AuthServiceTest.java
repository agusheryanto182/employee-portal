package com.employeeportal.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.employeeportal.common.ClientInfo;
import com.employeeportal.loginhistory.LoginFailureReason;
import com.employeeportal.loginhistory.LoginHistoryService;
import com.employeeportal.support.MutableClock;
import com.employeeportal.support.TestProperties;
import com.employeeportal.user.Role;
import com.employeeportal.user.User;
import com.employeeportal.user.UserRepository;
import com.employeeportal.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // Low cost factor keeps the unit tests fast; production uses the default (10).
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    private static final ClientInfo CLIENT = new ClientInfo("10.0.0.1", "JUnit");

    @Mock
    UserRepository userRepository;
    @Mock
    AccessTokenService accessTokenService;
    @Mock
    RefreshTokenService refreshTokenService;
    @Mock
    LoginHistoryService loginHistoryService;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private LoginRateLimiter rateLimiter;
    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        rateLimiter = new LoginRateLimiter(TestProperties.defaults(), clock);
        authService = new AuthService(userRepository, ENCODER, accessTokenService, refreshTokenService,
                loginHistoryService, rateLimiter);
        user = User.active("budi@example.com", "Budi", Role.EMPLOYEE, ENCODER.encode("correct-password"));
        user.setId(UUID.randomUUID());
    }

    private void givenUserExists() {
        when(userRepository.findByEmail("budi@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void successfulLoginIssuesTokensAndRecordsSuccess() {
        givenUserExists();
        when(refreshTokenService.issue(user)).thenReturn("refresh");
        when(accessTokenService.issue(user))
                .thenReturn(new AccessTokenService.AccessToken("access", clock.instant().plusSeconds(900), 900));

        AuthService.Session session = authService.login(new LoginRequest("  Budi@Example.COM ", "correct-password"), CLIENT, LoginPortal.EMPLOYEE);

        assertThat(session.accessToken()).isEqualTo("access");
        assertThat(session.refreshToken()).isEqualTo("refresh");
        assertThat(session.expiresInSeconds()).isEqualTo(900);
        verify(loginHistoryService).recordSuccess(user, CLIENT);
    }

    @Test
    void wrongPasswordIsRejectedAndRecorded() {
        givenUserExists();

        assertThatThrownBy(() -> authService.login(new LoginRequest("budi@example.com", "wrong"), CLIENT, LoginPortal.EMPLOYEE))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(loginHistoryService).recordFailure(user, "budi@example.com", LoginFailureReason.INVALID_CREDENTIALS, CLIENT);
        verify(refreshTokenService, never()).issue(any());
    }

    @Test
    void unknownEmailGetsTheSameErrorAndIsRecordedWithoutUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "whatever"), CLIENT, LoginPortal.EMPLOYEE))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verify(loginHistoryService).recordFailure(isNull(), eq("ghost@example.com"),
                eq(LoginFailureReason.INVALID_CREDENTIALS), eq(CLIENT));
    }

    @Test
    void pendingUserCannotSignIn() {
        User pending = User.pending("budi@example.com", "Budi", null, Role.EMPLOYEE);
        when(userRepository.findByEmail("budi@example.com")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> authService.login(new LoginRequest("budi@example.com", "anything"), CLIENT, LoginPortal.EMPLOYEE))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(loginHistoryService).recordFailure(pending, "budi@example.com", LoginFailureReason.ACCOUNT_PENDING, CLIENT);
    }

    @Test
    void inactiveUserWithCorrectPasswordCannotSignIn() {
        user.setStatus(UserStatus.INACTIVE);
        givenUserExists();

        assertThatThrownBy(() -> authService.login(new LoginRequest("budi@example.com", "correct-password"), CLIENT, LoginPortal.EMPLOYEE))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(loginHistoryService).recordFailure(user, "budi@example.com", LoginFailureReason.ACCOUNT_INACTIVE, CLIENT);
    }

    @Test
    void tooManyFailuresBlockEvenTheCorrectPassword() {
        givenUserExists();
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("budi@example.com", "wrong"), CLIENT, LoginPortal.EMPLOYEE))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(() -> authService.login(new LoginRequest("budi@example.com", "correct-password"), CLIENT, LoginPortal.EMPLOYEE))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        verify(loginHistoryService).recordFailure(user, "budi@example.com", LoginFailureReason.RATE_LIMITED, CLIENT);
    }

    @Test
    void employeeCannotSignInOnTheAdminLoginPage() {
        givenUserExists();

        assertThatThrownBy(() -> authService.login(new LoginRequest("budi@example.com", "correct-password"), CLIENT, LoginPortal.ADMIN))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verify(loginHistoryService).recordFailure(user, "budi@example.com", LoginFailureReason.WRONG_PORTAL, CLIENT);
        verify(refreshTokenService, never()).issue(any());
    }

    @Test
    void adminCannotSignInOnTheEmployeeLoginPage() {
        User admin = User.active("boss@example.com", "Boss", Role.ADMIN, ENCODER.encode("correct-password"));
        when(userRepository.findByEmail("boss@example.com")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.login(new LoginRequest("boss@example.com", "correct-password"), CLIENT, LoginPortal.EMPLOYEE))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(loginHistoryService).recordFailure(admin, "boss@example.com", LoginFailureReason.WRONG_PORTAL, CLIENT);
    }

    @Test
    void wrongPasswordOnTheWrongPortalIsRecordedAsInvalidCredentials() {
        givenUserExists();

        assertThatThrownBy(() -> authService.login(new LoginRequest("budi@example.com", "wrong"), CLIENT, LoginPortal.ADMIN))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(loginHistoryService).recordFailure(user, "budi@example.com", LoginFailureReason.INVALID_CREDENTIALS, CLIENT);
    }

    @Test
    void logoutWithoutCookieDoesNothing() {
        authService.logout(null);
        verify(refreshTokenService, never()).revoke(any());
    }
}
