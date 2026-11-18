package com.employeeportal.common;

import java.util.UUID;

import org.springframework.security.core.Authentication;

/** The signed-in user's id: the JWT subject, exposed by Spring Security as the principal name. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
