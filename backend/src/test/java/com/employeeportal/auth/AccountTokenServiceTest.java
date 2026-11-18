package com.employeeportal.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.employeeportal.common.TokenHasher;
import com.employeeportal.mail.AccountEmailRequested;
import com.employeeportal.support.MutableClock;
import com.employeeportal.support.TestProperties;
import com.employeeportal.user.Role;
import com.employeeportal.user.User;
import com.employeeportal.user.UserRepository;
import com.employeeportal.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class AccountTokenServiceTest {

    @Mock
    AccountTokenRepository tokenRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    RefreshTokenService refreshTokenService;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    ApplicationEventPublisher events;

    final MutableClock clock = new MutableClock(Instant.parse("2026-11-18T08:00:00Z"));
    AccountTokenService service;
    User pending;
    User active;

    @BeforeEach
    void setUp() {
        service = new AccountTokenService(
                tokenRepository, userRepository, refreshTokenService, passwordEncoder, events, TestProperties.defaults(), clock);
        pending = User.pending("new@example.com", "New User", null, Role.EMPLOYEE);
        pending.setId(UUID.randomUUID());
        active = User.active("budi@example.com", "Budi", Role.EMPLOYEE, "old-hash");
        active.setId(UUID.randomUUID());
    }

    /** Stores a token of the given type for the user and returns its plain value. */
    private String existingToken(User user, AccountTokenType type, Duration validFor) {
        String plain = TokenHasher.generateToken();
        AccountToken token = new AccountToken(user.getId(), type, TokenHasher.sha256(plain), clock.instant().plus(validFor));
        ReflectionTestUtils.setField(token, "id", UUID.randomUUID());
        when(tokenRepository.findByTokenHash(TokenHasher.sha256(plain))).thenReturn(Optional.of(token));
        lenient().when(userRepository.findById(user.getId())).thenReturn(Optional.of(user)); // unused for expired links
        return plain;
    }

    @Test
    void sendInviteReplacesOlderLinksStoresOnlyTheHashAndPublishesTheEmail() {
        String link = service.sendInvite(pending);

        assertThat(link).startsWith("http://localhost:5173/activate?token=");
        String plain = link.substring(link.indexOf("token=") + 6);
        verify(tokenRepository).invalidateAllForUser(pending.getId(), clock.instant());

        ArgumentCaptor<AccountToken> saved = ArgumentCaptor.forClass(AccountToken.class);
        verify(tokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(TokenHasher.sha256(plain)).isNotEqualTo(plain);
        assertThat(saved.getValue().getType()).isEqualTo(AccountTokenType.INVITE);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofHours(24)));

        ArgumentCaptor<AccountEmailRequested> email = ArgumentCaptor.forClass(AccountEmailRequested.class);
        verify(events).publishEvent(email.capture());
        assertThat(email.getValue().to()).isEqualTo("new@example.com");
        assertThat(email.getValue().link()).isEqualTo(link);
        assertThat(email.getValue().kind()).isEqualTo(AccountEmailRequested.Kind.INVITE);
    }

    @Test
    void resetLinksExpireAfterOneHour() {
        String link = service.sendPasswordReset(active);

        assertThat(link).startsWith("http://localhost:5173/reset-password?token=");
        ArgumentCaptor<AccountToken> saved = ArgumentCaptor.forClass(AccountToken.class);
        verify(tokenRepository).save(saved.capture());
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofHours(1)));
    }

    @Test
    void inspectDescribesAUsableLink() {
        String token = existingToken(pending, AccountTokenType.INVITE, Duration.ofHours(24));

        assertThat(service.inspect(token))
                .isEqualTo(new AccountTokenInfo(AccountTokenType.INVITE, "new@example.com", "New User"));
    }

    @Test
    void expiredUnknownOrWrongStatusLinksAreRejected() {
        String expired = existingToken(pending, AccountTokenType.INVITE, Duration.ofHours(24));
        clock.advance(Duration.ofHours(24));
        assertThatThrownBy(() -> service.inspect(expired)).isInstanceOf(InvalidAccountTokenException.class);

        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.inspect("unknown")).isInstanceOf(InvalidAccountTokenException.class);

        String inviteForActiveUser = existingToken(active, AccountTokenType.INVITE, Duration.ofHours(24));
        assertThatThrownBy(() -> service.inspect(inviteForActiveUser)).isInstanceOf(InvalidAccountTokenException.class);
    }

    @Test
    void activateSetsThePasswordAndActivatesTheAccount() {
        String token = existingToken(pending, AccountTokenType.INVITE, Duration.ofHours(24));
        when(tokenRepository.consume(any(), eq(clock.instant()))).thenReturn(1);
        when(passwordEncoder.encode("NewPassword1")).thenReturn("new-hash");

        service.activate(token, "NewPassword1");

        assertThat(pending.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(pending.getPasswordHash()).isEqualTo("new-hash");
        verify(tokenRepository).invalidateAllForUser(pending.getId(), clock.instant());
    }

    @Test
    void aLinkCannotBeUsedTwice() {
        String token = existingToken(pending, AccountTokenType.INVITE, Duration.ofHours(24));
        when(tokenRepository.consume(any(), any())).thenReturn(0); // someone else consumed it first

        assertThatThrownBy(() -> service.activate(token, "NewPassword1")).isInstanceOf(InvalidAccountTokenException.class);
        assertThat(pending.getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    void aResetLinkCannotActivateAndAnInviteCannotReset() {
        String reset = existingToken(active, AccountTokenType.RESET, Duration.ofHours(1));

        assertThatThrownBy(() -> service.activate(reset, "NewPassword1")).isInstanceOf(InvalidAccountTokenException.class);
        verify(tokenRepository, never()).consume(any(), any());
    }

    @Test
    void resetPasswordReplacesThePasswordAndSignsOutEverywhere() {
        String token = existingToken(active, AccountTokenType.RESET, Duration.ofHours(1));
        when(tokenRepository.consume(any(), any())).thenReturn(1);
        when(passwordEncoder.encode("NewPassword1")).thenReturn("new-hash");

        service.resetPassword(token, "NewPassword1");

        assertThat(active.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenService).revokeAll(active.getId());
    }
}
