package com.healthlink.service.auth;

import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.service.notification.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PasswordResetService
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private String testEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = new Patient();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(testEmail);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setIsActive(true);

        ReflectionTestUtils.setField(passwordResetService, "tokenValidityMinutes", 15);
    }

    @Test
    void initiatePasswordReset_shouldGenerateTokenAndSendEmail() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(otpService.generateOtp(eq(testEmail))).thenReturn("123456");
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());

        String resetToken = passwordResetService.initiatePasswordReset(testEmail);

        assertThat(resetToken).isNotNull().isNotEmpty();
        verify(userRepository).findByEmail(testEmail);
        verify(otpService).generateOtp(eq(testEmail));
    }

    @Test
    void initiatePasswordReset_shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.initiatePasswordReset(testEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(otpService, never()).generateOtp(anyString());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void initiatePasswordReset_shouldThrowWhenUserInactive() {
        testUser.setIsActive(false);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> passwordResetService.initiatePasswordReset(testEmail))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inactive account");

        verify(otpService, never()).generateOtp(anyString());
    }

    @Test
    void verifyResetOtp_shouldReturnTrueForValidOtp() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(otpService.generateOtp(anyString())).thenReturn("123456");

        String resetToken = passwordResetService.initiatePasswordReset(testEmail);

        when(otpService.verifyOtp(testEmail, "123456")).thenReturn(true);

        boolean verified = passwordResetService.verifyResetOtp(resetToken, "123456");

        assertThat(verified).isTrue();
        verify(otpService).verifyOtp(testEmail, "123456");
    }

    @Test
    void verifyResetOtp_shouldReturnFalseForInvalidToken() {
        boolean verified = passwordResetService.verifyResetOtp("invalid-token", "123456");

        assertThat(verified).isFalse();
        verify(otpService, never()).verifyOtp(anyString(), anyString());
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndRevokeTokens() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(otpService.generateOtp(anyString())).thenReturn("123456");

        String resetToken = passwordResetService.initiatePasswordReset(testEmail);

        when(otpService.verifyOtp(testEmail, "123456")).thenReturn(true);
        passwordResetService.verifyResetOtp(resetToken, "123456");

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("encoded-password");

        passwordResetService.resetPassword(resetToken, "NewPassword123!");

        assertThat(testUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(testUser.getTokensRevokedAt()).isNotNull();
        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(0);
        verify(userRepository).save(testUser);
        verify(emailService).sendPasswordResetConfirmation(testEmail, "John Doe");
    }

    @Test
    void resetPassword_shouldThrowWhenTokenNotVerified() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        String resetToken = passwordResetService.initiatePasswordReset(testEmail);

        assertThatThrownBy(() -> passwordResetService.resetPassword(resetToken, "NewPassword123!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OTP must be verified");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_shouldThrowForWeakPassword() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        String resetToken = passwordResetService.initiatePasswordReset(testEmail);

        when(otpService.verifyOtp(anyString(), anyString())).thenReturn(true);
        passwordResetService.verifyResetOtp(resetToken, "123456");

        // Add missing findById mock for resetPassword
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> passwordResetService.resetPassword(resetToken, "weak"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_shouldThrowForInvalidToken() {
        assertThatThrownBy(() -> passwordResetService.resetPassword("invalid-token", "NewPassword123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");
    }
}
