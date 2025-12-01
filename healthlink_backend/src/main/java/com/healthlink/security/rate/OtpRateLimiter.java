package com.healthlink.security.rate;

import com.healthlink.infrastructure.logging.SafeLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
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

    @Value("${healthlink.otp.redis-enabled:false}")
    private boolean redisEnabled;

    public OtpRateLimitResult consume(String email) {
        // If Redis-based OTP rate limiting is disabled, always allow (dev/local mode)
        if (!redisEnabled) {
            return OtpRateLimitResult.allowed(MAX_REQUESTS);
        }

        String key = buildKey(email);
        Long current;
        try {
            current = redisTemplate.opsForValue().increment(key);
            if (current != null && current == 1L) {
                redisTemplate.expire(key, WINDOW);
            }
        } catch (RedisConnectionFailureException ex) {
            // Redis is unavailable – log and allow request instead of failing with 500
            log.event("otp_rate_limiter_redis_unavailable")
                .withMasked("email", email)
                .with("error", ex.getMessage())
                .log();
            return OtpRateLimitResult.allowed(MAX_REQUESTS);
        } catch (DataAccessException ex) {
            // Generic Redis/data access problem – log and allow
            log.event("otp_rate_limiter_redis_unavailable")
                .withMasked("email", email)
                .with("error", ex.getMessage())
                .log();
            return OtpRateLimitResult.allowed(MAX_REQUESTS);
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
