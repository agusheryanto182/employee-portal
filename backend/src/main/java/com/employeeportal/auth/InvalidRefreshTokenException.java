package com.employeeportal.auth;

import org.springframework.http.HttpStatus;

import com.employeeportal.common.ApiException;

public class InvalidRefreshTokenException extends ApiException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED, "Session expired, please sign in again");
    }
}
