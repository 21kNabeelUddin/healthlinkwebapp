package com.healthlink.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Unified global exception handler for REST controllers.
 * Ensures consistent envelope and hides sensitive internal details.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String fieldName = ((FieldError) err).getField();
            String errorMessage = err.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return build(HttpStatus.BAD_REQUEST, "Validation Failed", "Invalid input data", errors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication Failed", "Invalid credentials", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Access Denied", "You don't have permission to access this resource", null);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Access Denied", "You don't have permission to perform this action", null);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        Map<String, String> details = new HashMap<>();
        details.put("retryAfterMillis", String.valueOf(ex.getRetryAfterMillis()));
        details.put("remaining", String.valueOf(ex.getRemaining()));
        return build(HttpStatus.TOO_MANY_REQUESTS, "Rate Limit Exceeded", ex.getMessage(), details);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String reason = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return build(status, status.getReasonPhrase(), reason, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, "Illegal State", ex.getMessage(), null);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("Runtime exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", sanitize(ex.getMessage()), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, Map<String, String> details) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .details(details)
                .build();
        return ResponseEntity.status(status).body(body);
    }

    private String sanitize(String raw) {
        if (raw == null) return "";
        // Basic PHI mitigation: never echo emails or phone patterns back in error messages
        return raw.replaceAll("[A-Za-z0-9+_.-]+@.+", "[redacted-email]")
                  .replaceAll("(\\+92|0)?3[0-9]{9}", "[redacted-phone]");
    }
}
