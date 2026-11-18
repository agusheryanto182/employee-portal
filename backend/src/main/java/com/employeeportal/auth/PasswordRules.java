package com.employeeportal.auth;

/** Shared password length rules. bcrypt only uses the first 72 bytes, so longer passwords are rejected. */
public final class PasswordRules {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    private PasswordRules() {
    }
}
