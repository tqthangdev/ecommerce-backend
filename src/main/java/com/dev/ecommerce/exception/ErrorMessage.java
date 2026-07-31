package com.dev.ecommerce.exception;

import org.springframework.http.HttpStatus;

public enum ErrorMessage {

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    ACCOUNT_NOT_ACTIVATED(HttpStatus.UNAUTHORIZED, "Account is not activated"),
    ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "Account is locked"),
    // %d is filled in with the remaining minutes at throw time — see
    // GlobalExceptionHandler.handleAccountLocked()
    ACCOUNT_LOCKED_WITH_TIME(HttpStatus.UNAUTHORIZED, "Account is locked. Try again in %d minute(s)."),
    ACCOUNT_EXPIRED(HttpStatus.UNAUTHORIZED, "Account has expired"),
    CREDENTIALS_EXPIRED(HttpStatus.UNAUTHORIZED, "Credentials have expired, please reset your password"),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "Authentication failed"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String message;

    ErrorMessage(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    /** For messages with a %d/%s placeholder, e.g. ACCOUNT_LOCKED_WITH_TIME. */
    public String format(Object... args) {
        return String.format(message, args);
    }
}