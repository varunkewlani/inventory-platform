package com.uphead.inventory.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for exceptions that carry an HTTP status and a machine-readable error
 * code, so {@link GlobalExceptionHandler} can translate them into the
 * standard {@code {"success": false, "error": {...}}} envelope without
 * per-exception branching.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
