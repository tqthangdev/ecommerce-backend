package com.dev.ecommerce.exception;

import org.springframework.http.HttpStatus;

public enum ErrorMessage {

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    ACCOUNT_NOT_ACTIVATED(HttpStatus.UNAUTHORIZED, "Account is not activated"),
    ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "Account is locked"),
    ACCOUNT_LOCKED_WITH_TIME(HttpStatus.UNAUTHORIZED, "Account is locked. Try again in %d minute(s)."),
    ACCOUNT_EXPIRED(HttpStatus.UNAUTHORIZED, "Account has expired"),
    CREDENTIALS_EXPIRED(HttpStatus.UNAUTHORIZED, "Credentials have expired, please reset your password"),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "Authentication failed"),

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),


    // ==========================
    // User management
    // ==========================

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "User not found"
    ),

    EMAIL_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Email already exists"
    ),

    ROLE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Role not found"
    ),


    // ==========================
    // Owner protection
    // ==========================

    OWNER_ACCOUNT_MODIFICATION_DENIED(
            HttpStatus.FORBIDDEN,
            "Cannot modify owner account"
    ),

    OWNER_DISABLE_DENIED(
            HttpStatus.FORBIDDEN,
            "Owner account cannot be disabled"
    ),

    OWNER_ROLE_ASSIGNMENT_DENIED(
            HttpStatus.FORBIDDEN,
            "Only owner can assign owner role"
    ),

    OWNER_CREATION_DENIED(
            HttpStatus.FORBIDDEN,
            "Only owner can create owner account"
    ),

    OWNER_ROLE_REMOVAL_DENIED(
            HttpStatus.FORBIDDEN,
            "Owner role cannot be removed"
    ),


    // ==========================
    // Admin protection
    // ==========================

    ADMIN_ROLE_ASSIGNMENT_DENIED(
            HttpStatus.FORBIDDEN,
            "Only owner can assign admin role"
    );


    private final HttpStatus status;
    private final String message;


    ErrorMessage(
            HttpStatus status,
            String message
    ) {
        this.status = status;
        this.message = message;
    }


    public HttpStatus getStatus() {
        return status;
    }


    public String getMessage() {
        return message;
    }


    public String format(Object... args) {
        return String.format(message, args);
    }
}