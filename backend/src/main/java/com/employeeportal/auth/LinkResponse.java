package com.employeeportal.auth;

/** A one-time link returned to the admin as a "copy link" fallback when the email does not arrive. */
public record LinkResponse(String link) {
}
