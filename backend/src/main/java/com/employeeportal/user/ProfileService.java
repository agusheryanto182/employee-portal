package com.employeeportal.user;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.employeeportal.common.NotSignedInException;

import lombok.RequiredArgsConstructor;

/** The signed-in user's own profile, for both roles. */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse get(UUID userId) {
        return UserResponse.from(findActive(userId));
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateProfileRequest request) {
        User user = findActive(userId);
        if (request.fullName() != null) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().isBlank() ? null : request.phone().trim());
        }
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    /**
     * Access tokens stay valid for up to 15 minutes after a user is deactivated, so self-service
     * changes check the account again.
     */
    private User findActive(UUID userId) {
        return userRepository.findById(userId).filter(User::isActive).orElseThrow(NotSignedInException::new);
    }
}
