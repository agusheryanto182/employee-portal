package com.employeeportal.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.employeeportal.common.TokenHasher;
import com.employeeportal.config.AppProperties;
import com.employeeportal.mail.AccountEmailRequested;
import com.employeeportal.user.User;
import com.employeeportal.user.UserRepository;
import com.employeeportal.user.UserStatus;

import lombok.RequiredArgsConstructor;

/**
 * One-time invite and password-reset links (docs/decisions/0004-invite-link-onboarding.md).
 *
 * <p>A link is usable when it is not used or replaced, not expired, and the account still has the
 * status the link is for: an invite only works while the user is {@code PENDING}, a reset only while
 * they are {@code ACTIVE}. Every failure gives the same error.
 */
@Service
@RequiredArgsConstructor
public class AccountTokenService {

    private final AccountTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final AppProperties properties;
    private final Clock clock;

    /** Replaces any earlier link, emails a new invite after commit and returns the link. */
    @Transactional
    public String sendInvite(User user) {
        return send(user, AccountTokenType.INVITE, AccountEmailRequested.Kind.INVITE, properties.accountToken().inviteTtl());
    }

    /** Replaces any earlier link, emails a reset link after commit and returns the link. */
    @Transactional
    public String sendPasswordReset(User user) {
        return send(user, AccountTokenType.RESET, AccountEmailRequested.Kind.PASSWORD_RESET, properties.accountToken().resetTtl());
    }

    /** Makes every unused link of the user stop working (deactivation, email change). */
    @Transactional
    public void invalidateAll(User user) {
        tokenRepository.invalidateAllForUser(user.getId(), clock.instant());
    }

    @Transactional(readOnly = true)
    public AccountTokenInfo inspect(String token) {
        Usable usable = findUsable(token);
        return new AccountTokenInfo(usable.token().getType(), usable.user().getEmail(), usable.user().getFullName());
    }

    /** Sets the first password of an invited user and activates the account. */
    @Transactional
    public void activate(String token, String password) {
        Usable usable = consume(token, AccountTokenType.INVITE);
        User user = usable.user();
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE); // saved on commit (dirty checking)
    }

    /** Replaces the password and signs the user out of every device. */
    @Transactional
    public void resetPassword(String token, String password) {
        Usable usable = consume(token, AccountTokenType.RESET);
        User user = usable.user();
        user.setPasswordHash(passwordEncoder.encode(password));
        refreshTokenService.revokeAll(user.getId());
    }

    private String send(User user, AccountTokenType type, AccountEmailRequested.Kind kind, Duration ttl) {
        Instant now = clock.instant();
        tokenRepository.invalidateAllForUser(user.getId(), now);
        String token = TokenHasher.generateToken();
        tokenRepository.save(new AccountToken(user.getId(), type, TokenHasher.sha256(token), now.plus(ttl)));

        String link = properties.baseUrl() + type.frontendPath() + "?token=" + token;
        events.publishEvent(new AccountEmailRequested(kind, user.getEmail(), user.getFullName(), link, ttl));
        return link;
    }

    private Usable consume(String token, AccountTokenType expectedType) {
        Usable usable = findUsable(token);
        if (usable.token().getType() != expectedType
                || tokenRepository.consume(usable.token().getId(), clock.instant()) == 0) {
            throw new InvalidAccountTokenException();
        }
        // Any other link of this user (for example an older reset email) stops working too.
        tokenRepository.invalidateAllForUser(usable.user().getId(), clock.instant());
        return usable;
    }

    private Usable findUsable(String token) {
        AccountToken accountToken = tokenRepository.findByTokenHash(TokenHasher.sha256(token))
                .filter(candidate -> candidate.isUsable(clock.instant()))
                .orElseThrow(InvalidAccountTokenException::new);
        User user = userRepository.findById(accountToken.getUserId())
                .filter(candidate -> candidate.getStatus() == accountToken.getType().requiredStatus())
                .orElseThrow(InvalidAccountTokenException::new);
        return new Usable(accountToken, user);
    }

    private record Usable(AccountToken token, User user) {
    }
}
