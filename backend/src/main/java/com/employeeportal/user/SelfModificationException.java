package com.employeeportal.user;

import org.springframework.http.HttpStatus;

import com.employeeportal.common.ApiException;

/** An admin may not deactivate themselves or change their own role, so the backoffice never loses its last admin. */
public class SelfModificationException extends ApiException {

    public SelfModificationException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
