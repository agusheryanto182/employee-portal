package com.employeeportal.auth;

/** What the activate and reset pages show before asking for a password. */
public record AccountTokenInfo(AccountTokenType type, String email, String fullName) {
}
