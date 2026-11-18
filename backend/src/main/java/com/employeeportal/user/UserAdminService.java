package com.employeeportal.user;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.employeeportal.auth.RefreshTokenService;
import com.employeeportal.common.PageResponse;

import lombok.RequiredArgsConstructor;

/** Backoffice user management. Users are never hard-deleted (docs/decisions/0006-no-hard-delete.md). */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

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

    /** Creates a {@code PENDING} user. The invite email is added in issue #7. */
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        User user = User.pending(email, request.fullName().trim(), blankToNull(request.phone()), request.role());
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    /**
     * Applies the non-null fields. A role change signs the user out everywhere, because their
     * sessions were issued for the old role.
     */
    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request, UUID actingAdminId) {
        User user = find(id);

        if (request.email() != null) {
            String email = User.normalizeEmail(request.email());
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new EmailAlreadyUsedException();
            }
            user.setEmail(email);
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
        return UserResponse.from(userRepository.saveAndFlush(user));
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

    private User find(UUID id) {
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
