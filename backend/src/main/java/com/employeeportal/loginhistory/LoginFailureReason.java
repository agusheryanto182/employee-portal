package com.employeeportal.loginhistory;

/** Stored for admins and the account owner. The login response itself never reveals it. */
public enum LoginFailureReason {
    INVALID_CREDENTIALS,
    ACCOUNT_PENDING,
    ACCOUNT_INACTIVE,
    /** Correct credentials, but signed in on the other area's login page (docs/decisions/0012). */
    WRONG_PORTAL,
    RATE_LIMITED
}
