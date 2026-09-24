package com.ecommerce.poc.dto;

import java.time.Instant;
import java.util.Map;

public class ApiErrorResponse {
    private Instant timestamp;
    private int status;
    private String code;
    private String message;
    private Map<String, String> fieldErrors;

    public ApiErrorResponse() {}

    public ApiErrorResponse(Instant timestamp, int status, String code, String message, Map<String, String> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }

    public Map<String, String> getError() {
        return Map.of("code", code != null ? code : "INTERNAL_ERROR", "message", message != null ? message : "An error occurred.");
    }
}
