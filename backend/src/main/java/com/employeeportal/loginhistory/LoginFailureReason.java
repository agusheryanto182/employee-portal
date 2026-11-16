package com.employeeportal.loginhistory;

/** Stored for admins and the account owner. The login response itself never reveals it. */
public enum LoginFailureReason {
    INVALID_CREDENTIALS,
    ACCOUNT_PENDING,
    ACCOUNT_INACTIVE,
    RATE_LIMITED
}
