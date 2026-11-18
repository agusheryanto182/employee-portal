package com.employeeportal.user;

import org.springframework.http.HttpStatus;

import com.employeeportal.common.ApiException;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND, "User not found");
    }
}
