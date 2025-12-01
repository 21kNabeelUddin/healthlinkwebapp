package com.healthlink.service.auth;

import com.healthlink.infrastructure.logging.SafeLogger;
import com.healthlink.service.notification.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
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
    private final EmailService emailService;
    private static final String OTP_PREFIX = "otp:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp_attempts:";
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRATION_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final SecureRandom random = new SecureRandom();

    @Value("${healthlink.otp.redis-enabled:false}")
    private boolean redisEnabled;

    @Value("${healthlink.otp.email-enabled:false}")
    private boolean emailEnabled;

    /**
     * Generate and store OTP for email
     */
    public String generateOtp(String email) {
        String otp = String.format("%0" + OTP_LENGTH + "d", random.nextInt((int) Math.pow(10, OTP_LENGTH)));

        // If Redis-backed OTP is disabled, treat this as dev mode: just email/log the OTP.
        if (!redisEnabled) {
            SafeLogger.get(OtpService.class)
                    .event("otp_generated_dev_mode")
                    .withMasked("email", email)
                    .with("otp", otp)
                    .log();
            sendOtpEmailIfEnabled(email, otp);
            return otp;
        }

        // Check rate limiting
        String attemptsKey = OTP_ATTEMPTS_PREFIX + email;
        String attempts;
        try {
            attempts = redisTemplate.opsForValue().get(attemptsKey);
        } catch (RedisConnectionFailureException ex) {
            SafeLogger.get(OtpService.class)
                    .event("otp_redis_unavailable")
                    .withMasked("email", email)
                    .with("error", ex.getMessage())
                    .log();
            // Fallback to dev behavior when Redis is unavailable
            sendOtpEmailIfEnabled(email, otp);
            return otp;
        } catch (DataAccessException ex) {
            SafeLogger.get(OtpService.class)
                    .event("otp_redis_unavailable")
                    .withMasked("email", email)
                    .with("error", ex.getMessage())
                    .log();
            // Fallback to dev behavior when Redis is unavailable
            sendOtpEmailIfEnabled(email, otp);
            return otp;
        }

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
        sendOtpEmailIfEnabled(email, otp);
        return otp;
    }

    /**
     * Verify OTP for email
     */
    public boolean verifyOtp(String email, String otp) {
        // In dev mode (Redis disabled), we don't actually persist OTPs – just allow verification
        if (!redisEnabled) {
            SafeLogger.get(OtpService.class)
                    .event("otp_verified_dev_mode")
                    .withMasked("email", email)
                    .with("provided_otp", otp)
                    .log();
            return true;
        }

        String key = OTP_PREFIX + email;
        String storedOtp;
        try {
            storedOtp = redisTemplate.opsForValue().get(key);
        } catch (RedisConnectionFailureException ex) {
            // Redis is down/unreachable – treat as verification failure instead of throwing
            SafeLogger.get(OtpService.class)
                    .event("otp_redis_unavailable_verify")
                    .withMasked("email", email)
                    .with("error", ex.getMessage())
                    .log();
            return false;
        } catch (DataAccessException ex) {
            SafeLogger.get(OtpService.class)
                    .event("otp_redis_unavailable_verify")
                    .withMasked("email", email)
                    .with("error", ex.getMessage())
                    .log();
            return false;
        }

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
        if (!redisEnabled) {
            // Nothing to delete in dev mode
            return;
        }

        String key = OTP_PREFIX + email;
        try {
            redisTemplate.delete(key);
        } catch (RedisConnectionFailureException ex) {
            SafeLogger.get(OtpService.class)
                    .event("otp_redis_unavailable_delete")
                    .withMasked("email", email)
                    .with("error", ex.getMessage())
                    .log();
        } catch (DataAccessException ex) {
            SafeLogger.get(OtpService.class)
                    .event("otp_redis_unavailable_delete")
                    .withMasked("email", email)
                    .with("error", ex.getMessage())
                    .log();
        }
        SafeLogger.get(OtpService.class)
            .event("otp_deleted")
            .withMasked("email", email)
            .log();
    }

    /**
     * Check if OTP exists for email
     */
    public boolean otpExists(String email) {
        if (!redisEnabled) {
            // We don't persist OTPs in dev mode
            return false;
        }

        String key = OTP_PREFIX + email;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (RedisConnectionFailureException ex) {
            SafeLogger.get(OtpService.class)
                    .event("otp_redis_unavailable_exists")
                    .withMasked("email", email)
                    .with("error", ex.getMessage())
                    .log();
            return false;
        } catch (DataAccessException ex) {
            SafeLogger.get(OtpService.class)
                    .event("otp_redis_unavailable_exists")
                    .withMasked("email", email)
                    .with("error", ex.getMessage())
                    .log();
            return false;
        }
    }

    private void sendOtpEmailIfEnabled(String email, String otp) {
        if (!emailEnabled) {
            return;
        }
        try {
            emailService.sendSimpleEmail(
                    email,
                    "Your HealthLink verification code",
                    "Your one-time verification code is: " + otp + "\n\n" +
                            "This code will expire in " + OTP_EXPIRATION_MINUTES + " minutes.");
        } catch (Exception ex) {
            SafeLogger.get(OtpService.class)
                    .event("otp_email_send_failed")
                    .withMasked("email", email)
                    .with("error", ex.getMessage())
                    .log();
        }
    }
}
