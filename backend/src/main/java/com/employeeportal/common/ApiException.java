package com.employeeportal.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Base class for errors that map directly to an HTTP status and a client-safe message. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** Extra response headers, for example {@code Retry-After}. */
    public HttpHeaders getHeaders() {
        return HttpHeaders.EMPTY;
    }
}
