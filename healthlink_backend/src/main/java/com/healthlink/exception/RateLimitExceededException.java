package com.healthlink.exception;

/**
 * Thrown when a caller exceeds the configured global or route/role specific rate limit.
 * Handled by GlobalExceptionHandler returning HTTP 429 with retry metadata.
 */
public class RateLimitExceededException extends RuntimeException {
    private final long retryAfterMillis;
    private final long remaining;

    public RateLimitExceededException(String message, long retryAfterMillis, long remaining) {
        super(message);
        this.retryAfterMillis = retryAfterMillis;
        this.remaining = remaining;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }

    public long getRemaining() {
        return remaining;
    }
}
