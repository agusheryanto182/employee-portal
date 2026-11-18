package com.employeeportal.auth;

import com.employeeportal.user.Role;
import com.employeeportal.user.User;

/**
 * The login page an attempt came from. The dashboard and the backoffice have separate sign-in
 * pages, and each only accepts its own role (docs/decisions/0012-separate-admin-and-employee-areas.md).
 */
public enum LoginPortal {
    EMPLOYEE(Role.EMPLOYEE),
    ADMIN(Role.ADMIN);

    private final Role role;

    LoginPortal(Role role) {
        this.role = role;
    }

    public boolean accepts(User user) {
        return user.getRole() == role;
    }
}
