package com.healthlink.security.rate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtpRateLimitResult {
    private boolean allowed;
    private long remainingAttempts;
    private long retryAfterSeconds;

    // Backwards compatibility convenience accessor for legacy tests
    public boolean allowed() { return allowed; }

    public static OtpRateLimitResult allowed(long remaining) {
        return new OtpRateLimitResult(true, remaining, 0);
    }
    public static OtpRateLimitResult blocked(long remaining, long retryAfter) {
        return new OtpRateLimitResult(false, remaining, retryAfter);
    }
}
