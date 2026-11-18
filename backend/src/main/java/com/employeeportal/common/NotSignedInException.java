package com.employeeportal.common;

import org.springframework.http.HttpStatus;

/** The access token is valid, but its account no longer exists or is no longer active. */
public class NotSignedInException extends ApiException {

    public NotSignedInException() {
        super(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
