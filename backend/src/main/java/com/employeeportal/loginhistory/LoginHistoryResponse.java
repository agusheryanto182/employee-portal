package com.employeeportal.loginhistory;

import java.time.Instant;
import java.util.UUID;

public record LoginHistoryResponse(
        UUID id,
        UUID userId,
        String email,
        boolean success,
        LoginFailureReason failureReason,
        String ipAddress,
        String userAgent,
        Instant createdAt) {

    public static LoginHistoryResponse from(LoginHistory entry) {
        return new LoginHistoryResponse(
                entry.getId(),
                entry.getUserId(),
                entry.getEmailAttempted(),
                entry.isSuccess(),
                entry.getFailureReason(),
                entry.getIpAddress(),
                entry.getUserAgent(),
                entry.getCreatedAt());
    }
}
