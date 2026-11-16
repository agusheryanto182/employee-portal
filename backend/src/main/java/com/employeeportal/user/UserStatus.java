package com.employeeportal.user;

/** Lifecycle: PENDING (invited) -> ACTIVE (password set) <-> INACTIVE (deactivated by an admin). */
public enum UserStatus {
    PENDING,
    ACTIVE,
    INACTIVE
}
