package com.employeeportal.auth;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.employeeportal.common.FieldErrorException;
import com.employeeportal.common.NotSignedInException;
import com.employeeportal.user.User;
import com.employeeportal.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Self-service password change. Every session of the user is revoked and a new one is issued for
 * the device that made the change, so it stays signed in while all other devices are signed out.
 *
 * <p>Revoking everything and re-issuing is simpler than "revoke all but this one": the refresh
 * cookie is scoped to {@code /api/auth}, so this request does not carry it and cannot tell which
 * session is the current one.
 */
@Service
@RequiredArgsConstructor
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;
    private final AccountTokenService accountTokenService;

    @Transactional
    public AuthService.Session change(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).filter(User::isActive).orElseThrow(NotSignedInException::new);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new FieldErrorException("currentPassword", "is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new FieldErrorException("newPassword", "must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        refreshTokenService.revokeAll(user.getId());
        accountTokenService.invalidateAll(user); // a pending reset link must not undo this change

        AccessTokenService.AccessToken accessToken = accessTokenService.issue(user);
        return new AuthService.Session(
                accessToken.value(), accessToken.expiresInSeconds(), refreshTokenService.issue(user), user);
    }
}
