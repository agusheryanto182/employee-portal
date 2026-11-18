package com.employeeportal.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.employeeportal.common.TokenHasher;
import com.employeeportal.support.MutableClock;
import com.employeeportal.support.TestProperties;
import com.employeeportal.user.Role;
import com.employeeportal.user.User;
import com.employeeportal.user.UserRepository;
import com.employeeportal.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    RefreshTokenRepository repository;
    @Mock
    UserRepository userRepository;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, userRepository, TestProperties.defaults(), clock);
        user = User.active("budi@example.com", "Budi", Role.EMPLOYEE, "hash");
        user.setId(UUID.randomUUID());
    }

    private RefreshToken stored(String plain) {
        RefreshToken token = new RefreshToken(user.getId(), TokenHasher.sha256(plain), clock.instant().plus(Duration.ofDays(7)));
        when(repository.findByTokenHash(TokenHasher.sha256(plain))).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    void rotationRevokesTheOldTokenAndIssuesANewOne() {
        RefreshToken old = stored("old-token");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        RefreshTokenService.Rotation rotation = service.rotate("old-token");

        assertThat(old.isRevoked()).isTrue();
        assertThat(rotation.user()).isSameAs(user);
        assertThat(rotation.refreshToken()).isNotEqualTo("old-token").hasSize(43);
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void unknownTokenIsRejected() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("nope")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        stored("old-token");
        clock.advance(Duration.ofDays(7));

        assertThatThrownBy(() -> service.rotate("old-token")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void reuseWithinGraceIsRejectedWithoutRevokingOtherSessions() {
        RefreshToken old = stored("old-token");
        old.revoke(clock.instant());
        clock.advance(Duration.ofSeconds(10));

        assertThatThrownBy(() -> service.rotate("old-token")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(repository, never()).revokeAllForUser(any(), any());
    }

    @Test
    void reuseAfterGraceRevokesEverySessionOfTheUser() {
        RefreshToken old = stored("old-token");
        old.revoke(clock.instant());
        clock.advance(Duration.ofMinutes(5));

        assertThatThrownBy(() -> service.rotate("old-token")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(repository).revokeAllForUser(user.getId(), clock.instant());
    }

    @Test
    void deactivatedUserCannotRefresh() {
        RefreshToken old = stored("old-token");
        user.setStatus(UserStatus.INACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.rotate("old-token")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(old.isRevoked()).isTrue();
    }
}
