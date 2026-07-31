package com.dev.ecommerce.exception;

import org.springframework.security.authentication.LockedException;

import java.time.LocalDateTime;

public class AccountLockedException extends LockedException {

    private final LocalDateTime lockedUntil;

    public AccountLockedException(String message, LocalDateTime lockedUntil) {
        super(message);
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
}