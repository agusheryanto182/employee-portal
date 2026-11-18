package com.employeeportal.auth;

import com.employeeportal.user.UserStatus;

/** What a one-time link is for, and which account status it applies to. */
public enum AccountTokenType {
    /** Sets the first password of an invited ({@code PENDING}) user. */
    INVITE(UserStatus.PENDING, "/activate"),
    /** Replaces the password of an {@code ACTIVE} user. */
    RESET(UserStatus.ACTIVE, "/reset-password");

    private final UserStatus requiredStatus;
    private final String frontendPath;

    AccountTokenType(UserStatus requiredStatus, String frontendPath) {
        this.requiredStatus = requiredStatus;
        this.frontendPath = frontendPath;
    }

    public UserStatus requiredStatus() {
        return requiredStatus;
    }

    public String frontendPath() {
        return frontendPath;
    }
}
