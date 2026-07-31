package com.dev.ecommerce.exception;

import com.dev.ecommerce.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------- Authentication-related (login) ----------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Login failed: bad credentials");
        return respond(ErrorMessage.INVALID_CREDENTIALS);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex) {
        log.warn("Login failed: account disabled/not activated");
        return respond(ErrorMessage.ACCOUNT_NOT_ACTIVATED);
    }

    // More specific than LockedException below — Spring picks this one when the
    // actual thrown type is AccountLockedException (thrown by CustomPreAuthenticationChecks).
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLocked(AccountLockedException ex) {
        long minutesLeft = Math.max(1, Duration.between(LocalDateTime.now(), ex.getLockedUntil()).toMinutes() + 1);
        log.warn("Login attempt on locked account, {} minute(s) remaining", minutesLeft);
        return ResponseEntity
                .status(ErrorMessage.ACCOUNT_LOCKED_WITH_TIME.getStatus())
                .body(ApiResponse.error(ErrorMessage.ACCOUNT_LOCKED_WITH_TIME.format(minutesLeft)));
    }

    // Fallback for any plain LockedException not carrying lock-expiry detail
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex) {
        log.warn("Login failed: account locked");
        return respond(ErrorMessage.ACCOUNT_LOCKED);
    }

    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountExpired(AccountExpiredException ex) {
        log.warn("Login failed: account expired");
        return respond(ErrorMessage.ACCOUNT_EXPIRED);
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleCredentialsExpired(CredentialsExpiredException ex) {
        log.warn("Login failed: credentials expired");
        return respond(ErrorMessage.CREDENTIALS_EXPIRED);
    }

    // Safety net for any other AuthenticationException subclass not handled above
    // (e.g. custom AuthenticationException implementations, future Spring Security additions)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getClass().getSimpleName());
        return respond(ErrorMessage.AUTHENTICATION_FAILED);
    }

    // ---------- Authorization ----------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return respond(ErrorMessage.ACCESS_DENIED);
    }

    // ---------- Business logic ----------

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ---------- Validation ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(message));
    }

    // ---------- Fallback ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(ErrorMessage.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> respond(ErrorMessage error) {
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.error(error.getMessage()));
    }
}