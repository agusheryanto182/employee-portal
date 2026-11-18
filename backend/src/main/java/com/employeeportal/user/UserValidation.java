package com.employeeportal.user;

final class UserValidation {

    /** Digits, spaces, dashes and parentheses, with an optional leading +. */
    static final String PHONE = "^\\+?[0-9 ()-]{6,30}$";
    static final String PHONE_OR_EMPTY = "^$|" + PHONE;
    static final String PHONE_MESSAGE = "must be a valid phone number";
    /** At least one non-whitespace character (null values are not checked). */
    static final String NOT_BLANK = "(?s).*\\S.*";

    private UserValidation() {
    }
}
