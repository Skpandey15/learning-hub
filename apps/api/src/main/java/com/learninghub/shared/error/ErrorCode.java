package com.learninghub.shared.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Invalid request", "The request failed validation."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request", "The request could not be parsed."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required", "Valid authentication is required."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied", "You are not permitted to perform this operation."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found", "The requested resource was not found."),
    CONFLICT(HttpStatus.CONFLICT, "Request conflict", "The request conflicts with current resource state."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded", "Too many requests were received."),
    DEPENDENCY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable", "A required service is temporarily unavailable."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "The request could not be completed.");

    private final HttpStatus status;
    private final String title;
    private final String detail;

    ErrorCode(HttpStatus status, String title, String detail) {
        this.status = status;
        this.title = title;
        this.detail = detail;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public String detail() {
        return detail;
    }

    public String type() {
        return "https://learninghub.dev/problems/" + name().toLowerCase().replace('_', '-');
    }
}
