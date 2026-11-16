package com.employeeportal.loginhistory;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.employeeportal.common.ClientInfo;
import com.employeeportal.user.User;

import lombok.RequiredArgsConstructor;

/**
 * Records login attempts. Each record is written in its own transaction (REQUIRES_NEW) so a
 * failed login, which ends in an exception, still leaves its history row behind.
 */
@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(User user, ClientInfo client) {
        repository.save(LoginHistory.success(user.getId(), user.getEmail(), client));
    }

    /**
     * @param user  the matching account, or null when the email is unknown
     * @param email the email as attempted (normalized)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(User user, String email, LoginFailureReason reason, ClientInfo client) {
        UUID userId = user == null ? null : user.getId();
        repository.save(LoginHistory.failure(userId, email, reason, client));
    }
}
