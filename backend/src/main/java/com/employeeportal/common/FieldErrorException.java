package com.employeeportal.common;

import java.util.Map;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * A 400 tied to one request field, reported in the same {@code errors} map as bean validation
 * failures so forms can show it next to the right input.
 */
@Getter
public class FieldErrorException extends ApiException {

    private final Map<String, String> errors;

    public FieldErrorException(String field, String message) {
        super(HttpStatus.BAD_REQUEST, "Validation failed");
        this.errors = Map.of(field, message);
    }
}
