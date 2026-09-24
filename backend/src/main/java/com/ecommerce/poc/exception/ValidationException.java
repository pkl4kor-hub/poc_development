package com.ecommerce.poc.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ValidationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
