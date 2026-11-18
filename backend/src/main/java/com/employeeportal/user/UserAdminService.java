package com.employeeportal.user;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.employeeportal.auth.AccountTokenService;
import com.employeeportal.auth.LinkResponse;
import com.employeeportal.auth.RefreshTokenService;
import com.employeeportal.common.ApiException;
import com.employeeportal.common.PageResponse;

import lombok.RequiredArgsConstructor;

/** Backoffice user management. Users are never hard-deleted (docs/decisions/0006-no-hard-delete.md). */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final AccountTokenService accountTokenService;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String search, UserStatus status, Role role, Pageable pageable) {
        return PageResponse.from(
                userRepository.findAll(UserSpecifications.matching(search, status, role), pageable),
                UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(find(id));
    }

    /** Creates a {@code PENDING} user and emails them an invite link once the transaction commits. */
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        User user = userRepository.saveAndFlush(
                User.pending(email, request.fullName().trim(), blankToNull(request.phone()), request.role()));
        accountTokenService.sendInvite(user);
        return UserResponse.from(user);
    }

    /**
     * Applies the non-null fields. A role change signs the user out everywhere, because their
     * sessions were issued for the old role. An email change cancels links sent to the old address;
     * a user who has not activated yet gets a new invite at the new address.
     */
    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request, UUID actingAdminId) {
        User user = find(id);
        boolean emailChanged = false;

        if (request.email() != null) {
            String email = User.normalizeEmail(request.email());
            if (!email.equals(user.getEmail())) {
                if (userRepository.existsByEmail(email)) {
                    throw new EmailAlreadyUsedException();
                }
                user.setEmail(email);
                emailChanged = true;
            }
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(blankToNull(request.phone()));
        }
        if (request.role() != null && request.role() != user.getRole()) {
            if (user.getId().equals(actingAdminId)) {
                throw new SelfModificationException("You cannot change your own role");
            }
            user.setRole(request.role());
            refreshTokenService.revokeAll(user.getId());
        }
        userRepository.saveAndFlush(user);
        if (emailChanged) {
            if (user.getStatus() == UserStatus.PENDING) {
                accountTokenService.sendInvite(user);
            } else {
                accountTokenService.invalidateAll(user);
            }
        }
        return UserResponse.from(user);
    }

    /** Blocks sign-in and revokes every session. Deactivating an inactive user changes nothing. */
    @Transactional
    public UserResponse deactivate(UUID id, UUID actingAdminId) {
        User user = find(id);
        if (user.getId().equals(actingAdminId)) {
            throw new SelfModificationException("You cannot deactivate your own account");
        }
        if (user.getStatus() != UserStatus.INACTIVE) {
            user.deactivate();
            refreshTokenService.revokeAll(user.getId());
            accountTokenService.invalidateAll(user);
        }
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    /** Reactivates an {@code INACTIVE} user. Other statuses are left as they are. */
    @Transactional
    public UserResponse activate(UUID id) {
        User user = find(id);
        if (user.getStatus() == UserStatus.INACTIVE) {
            user.reactivate();
        }
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    /** Sends a fresh invite to a user who has not activated yet; older invite links stop working. */
    @Transactional
    public LinkResponse resendInvite(UUID id) {
        User user = find(id);
        if (user.getStatus() != UserStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only users who have not activated their account can be invited");
        }
        return new LinkResponse(accountTokenService.sendInvite(user));
    }

    /** Emails a password reset link to an active user. Their password stays valid until the link is used. */
    @Transactional
    public LinkResponse sendPasswordReset(UUID id) {
        User user = find(id);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "Only active users can reset their password");
        }
        return new LinkResponse(accountTokenService.sendPasswordReset(user));
    }

    private User find(UUID id) {
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
