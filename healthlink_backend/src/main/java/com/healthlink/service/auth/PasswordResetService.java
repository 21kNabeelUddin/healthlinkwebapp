package com.healthlink.service.auth;

import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.infrastructure.logging.SafeLogger;
import com.healthlink.service.notification.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Password Reset Service
 * Handles secure password reset flow with OTP verification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // In-memory reset token store (consider Redis for production)
    private final Map<String, PasswordResetToken> resetTokens = new HashMap<>();

    @Value("${healthlink.security.password-reset-token-validity-minutes:15}")
    private int tokenValidityMinutes;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Initiate password reset by sending OTP to user's email
     */
    @Transactional(readOnly = true)
    public String initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        if (!user.getIsActive()) {
            throw new IllegalStateException("Cannot reset password for inactive account");
        }

        // Generate 6-digit OTP
        String otp = generateOtp();

        // Store OTP using existing OTP service
        otpService.generateOtp(email);

        // Create reset token
        String resetToken = UUID.randomUUID().toString();

        PasswordResetToken token = new PasswordResetToken(
                resetToken,
                user.getId(),
                email,
                otp,
                Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(tokenValidityMinutes)));

        resetTokens.put(resetToken, token);

        // Send OTP via email
        try {
            emailService.sendPasswordResetEmail(email, user.getFullName(), otp);
            SafeLogger.get(PasswordResetService.class)
                .event("password_reset_initiated")
                .withMasked("email", email)
                .log();
        } catch (Exception e) {
            SafeLogger.get(PasswordResetService.class)
                .event("password_reset_email_failed")
                .withMasked("email", email)
                .with("error", e.getClass().getSimpleName())
                .log();
            resetTokens.remove(resetToken);
            throw new RuntimeException("Failed to send password reset email", e);
        }

        // Cleanup expired tokens
        cleanupExpiredTokens();

        return resetToken;
    }

    /**
     * Verify OTP and allow password reset
     */
    public boolean verifyResetOtp(String resetToken, String otp) {
        PasswordResetToken token = resetTokens.get(resetToken);

        if (token == null) {
            SafeLogger.get(PasswordResetService.class)
                .event("password_reset_invalid_token")
                .with("token_prefix", resetToken.substring(0, Math.min(8, resetToken.length())))
                .log();
            return false;
        }

        if (token.isExpired()) {
            SafeLogger.get(PasswordResetService.class)
                .event("password_reset_expired_token")
                .with("token_prefix", resetToken.substring(0, Math.min(8, resetToken.length())))
                .log();
            resetTokens.remove(resetToken);
            return false;
        }

        boolean verified = otpService.verifyOtp(token.getEmail(), otp);

        if (verified) {
            token.setOtpVerified(true);
            SafeLogger.get(PasswordResetService.class)
                .event("password_reset_otp_verified")
                .withMasked("email", token.getEmail())
                .log();
        } else {
            SafeLogger.get(PasswordResetService.class)
                .event("password_reset_otp_invalid")
                .withMasked("email", token.getEmail())
                .log();
        }

        return verified;
    }

    /**
     * Complete password reset with new password
     */
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        PasswordResetToken token = resetTokens.get(resetToken);

        if (token == null || token.isExpired()) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        if (!token.isOtpVerified()) {
            throw new IllegalStateException("OTP must be verified before resetting password");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Validate password strength (add your validation here)
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        // Revoke all existing tokens for security
        user.markTokensRevoked();

        // Reset failed login attempts
        user.resetFailedLoginAttempts();

        userRepository.save(user);

        // Remove reset token
        resetTokens.remove(resetToken);

        // Send confirmation email
        try {
            emailService.sendPasswordResetConfirmation(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            SafeLogger.get(PasswordResetService.class)
                .event("password_reset_confirmation_email_failed")
                .withMasked("email", user.getEmail())
                .with("error", e.getClass().getSimpleName())
                .log();
            // Don't fail the operation if email fails
        }

        SafeLogger.get(PasswordResetService.class)
            .event("password_reset_completed")
            .withMasked("email", user.getEmail())
            .log();
    }

    /**
     * Generate 6-digit OTP
     */
    private String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(1000000));
    }

    /**
     * Clean up expired reset tokens
     */
    private void cleanupExpiredTokens() {
        resetTokens.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Inner class to represent password reset token.
     * Avoids Lombok so that all methods used above are explicitly defined.
     */
    private static class PasswordResetToken {
        private final String token;
        private final UUID userId;
        private final String email;
        private final String otp;
        private final Instant expiresAt;
        private boolean otpVerified = false;

        public PasswordResetToken(String token, UUID userId, String email, String otp, Instant expiresAt) {
            this.token = token;
            this.userId = userId;
            this.email = email;
            this.otp = otp;
            this.expiresAt = expiresAt;
        }

        public String getToken() {
            return token;
        }

        public UUID getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getOtp() {
            return otp;
        }

        public boolean isOtpVerified() {
            return otpVerified;
        }

        public void setOtpVerified(boolean otpVerified) {
            this.otpVerified = otpVerified;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
