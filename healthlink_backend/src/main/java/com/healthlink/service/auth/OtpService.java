package com.healthlink.service.auth;

import com.healthlink.infrastructure.logging.SafeLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * OTP Service for email-based OTP authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String OTP_PREFIX = "otp:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp_attempts:";
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRATION_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate and store OTP for email
     */
    public String generateOtp(String email) {
        // Check rate limiting
        String attemptsKey = OTP_ATTEMPTS_PREFIX + email;
        String attempts = redisTemplate.opsForValue().get(attemptsKey);

        int attemptCount = 0;
        try {
            attemptCount = attempts != null ? Integer.parseInt(attempts) : 0;
        } catch (NumberFormatException e) {
            SafeLogger.get(OtpService.class)
                .event("invalid_attempt_count")
                .withMasked("email", email)
                .log();
            redisTemplate.delete(attemptsKey);
        }

        if (attemptCount >= MAX_OTP_ATTEMPTS) {
            throw new RuntimeException("Too many OTP requests. Please try again later.");
        }

        // Generate 6-digit OTP
        String otp = String.format("%0" + OTP_LENGTH + "d", random.nextInt((int) Math.pow(10, OTP_LENGTH)));

        // Store in Redis with expiration
        String key = OTP_PREFIX + email;
        redisTemplate.opsForValue().set(key, otp, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // Increment attempts counter (expires in 1 hour)
        redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, Duration.ofHours(1));

        SafeLogger.get(OtpService.class)
            .event("otp_generated")
            .withMasked("email", email)
            .log();
        return otp;
    }

    /**
     * Verify OTP for email
     */
    public boolean verifyOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            SafeLogger.get(OtpService.class)
                .event("otp_not_found")
                .withMasked("email", email)
                .log();
            return false;
        }

        if (storedOtp.equals(otp)) {
            // OTP verified, delete it
            redisTemplate.delete(key);
            SafeLogger.get(OtpService.class)
                .event("otp_verified")
                .withMasked("email", email)
                .log();
            return true;
        }

        SafeLogger.get(OtpService.class)
            .event("otp_invalid")
            .withMasked("email", email)
            .log();
        return false;
    }

    /**
     * Delete OTP (if needed to cancel)
     */
    public void deleteOtp(String email) {
        String key = OTP_PREFIX + email;
        redisTemplate.delete(key);
        SafeLogger.get(OtpService.class)
            .event("otp_deleted")
            .withMasked("email", email)
            .log();
    }

    /**
     * Check if OTP exists for email
     */
    public boolean otpExists(String email) {
        String key = OTP_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
