package com.employeeportal.auth;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import com.employeeportal.common.ApiException;

import lombok.Getter;

@Getter
public class TooManyLoginAttemptsException extends ApiException {

    private final Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Too many failed login attempts, please try again later");
        this.retryAfter = retryAfter;
    }

    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, retryAfter.toSeconds())));
        return headers;
    }
}
