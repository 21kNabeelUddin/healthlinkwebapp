package com.healthlink.security.rate;

import com.healthlink.infrastructure.logging.SafeLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * OTP rate limiter enforcing max 5 OTP requests per email per rolling hour.
 */
@Service
@RequiredArgsConstructor
public class OtpRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofHours(1);
    private final StringRedisTemplate redisTemplate;
    private final SafeLogger log = SafeLogger.get(OtpRateLimiter.class);

    public OtpRateLimitResult consume(String email) {
        String key = buildKey(email);
        Long current = redisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redisTemplate.expire(key, WINDOW);
        }
        long remaining = MAX_REQUESTS - (current == null ? 0 : current);
        if (current != null && current > MAX_REQUESTS) {
            Long ttl = redisTemplate.getExpire(key);
            long retryAfterSeconds = ttl != null ? ttl : WINDOW.toSeconds();
            log.event("otp_rate_limited").withMasked("email", email).log();
            return OtpRateLimitResult.blocked(remaining < 0 ? 0 : remaining, retryAfterSeconds);
        }
        return OtpRateLimitResult.allowed(remaining);
    }

    private String buildKey(String email) {
        return "otp_rate:" + email.toLowerCase();
    }
}
