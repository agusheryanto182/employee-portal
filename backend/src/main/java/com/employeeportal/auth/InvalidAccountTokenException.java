package com.employeeportal.auth;

import org.springframework.http.HttpStatus;

import com.employeeportal.common.ApiException;

/** One message for unknown, expired, used and replaced links, so a link reveals nothing more. */
public class InvalidAccountTokenException extends ApiException {

    public InvalidAccountTokenException() {
        super(HttpStatus.BAD_REQUEST, "This link is invalid or has expired");
    }
}
