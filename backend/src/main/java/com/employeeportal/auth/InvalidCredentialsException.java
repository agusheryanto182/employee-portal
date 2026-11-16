package com.employeeportal.auth;

import org.springframework.http.HttpStatus;

import com.employeeportal.common.ApiException;

/** Same message for every failure reason, so the endpoint cannot be used to discover accounts. */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
