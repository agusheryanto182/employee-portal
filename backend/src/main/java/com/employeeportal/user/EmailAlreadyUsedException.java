package com.employeeportal.user;

import org.springframework.http.HttpStatus;

import com.employeeportal.common.ApiException;

public class EmailAlreadyUsedException extends ApiException {

    public EmailAlreadyUsedException() {
        super(HttpStatus.CONFLICT, "Email is already registered");
    }
}
