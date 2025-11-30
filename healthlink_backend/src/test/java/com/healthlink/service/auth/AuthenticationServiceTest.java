package com.healthlink.service.auth;

import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.dto.auth.LoginRequest;
import com.healthlink.security.jwt.JwtService;
import com.healthlink.security.token.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthenticationServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private OtpService otpService;
    private AuthenticationManager authManager;
    private UserDetailsService userDetailsService;
    private RefreshTokenService refreshTokenService;
    private com.healthlink.security.token.AccessTokenBlacklistService accessTokenBlacklistService;
    private com.healthlink.infrastructure.email.ApprovalEmailService approvalEmailService;

    private AuthenticationService service;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        otpService = mock(OtpService.class);
        authManager = mock(AuthenticationManager.class);
        userDetailsService = mock(UserDetailsService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        accessTokenBlacklistService = mock(com.healthlink.security.token.AccessTokenBlacklistService.class);
        approvalEmailService = mock(com.healthlink.infrastructure.email.ApprovalEmailService.class);
        service = new AuthenticationService(userRepository, passwordEncoder, jwtService, otpService, authManager,
                userDetailsService, refreshTokenService, accessTokenBlacklistService, approvalEmailService);
    }

    @Test
    void login_requiresPassword() {
        Patient p = new Patient();
        p.setEmail("user@example.com");
        p.setRole(UserRole.PATIENT);
        p.setApprovalStatus(com.healthlink.domain.user.enums.ApprovalStatus.APPROVED);
        p.setIsActive(true);
        p.setIsEmailVerified(true);
        p.setId(UUID.randomUUID());
        p.setId(UUID.randomUUID());
        p.setId(UUID.randomUUID());
        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(p));
        LoginRequest req = LoginRequest.builder().email("user@example.com").build();
        assertThrows(RuntimeException.class, () -> service.login(req));
    }

    @Test
    void login_patientWithOtpEnabled_requiresOtp() {
        Patient p = new Patient();
        p.setEmail("user@example.com");
        p.setRole(UserRole.PATIENT);
        p.setApprovalStatus(com.healthlink.domain.user.enums.ApprovalStatus.APPROVED);
        p.setIsActive(true);
        p.setIsEmailVerified(true);
        p.setId(UUID.randomUUID());
        p.setId(UUID.randomUUID());
        p.setOtpEnabled(true);
        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(p));
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(mock(UserDetails.class));
        LoginRequest req = LoginRequest.builder().email("user@example.com").password("secret").build();
        // OTP missing
        assertThrows(RuntimeException.class, () -> service.login(req));
        // With OTP invalid
        when(otpService.verifyOtp("user@example.com", "123456")).thenReturn(false);
        LoginRequest invalidOtp = LoginRequest.builder().email("user@example.com").password("secret").otp("123456")
                .build();
        assertThrows(RuntimeException.class, () -> service.login(invalidOtp));
    }

    @Test
    void login_patientWithOtpDisabled_noOtpNeeded() {
        Patient p = new Patient();
        p.setEmail("user@example.com");
        p.setRole(UserRole.PATIENT);
        p.setApprovalStatus(com.healthlink.domain.user.enums.ApprovalStatus.APPROVED);
        p.setIsActive(true);
        p.setIsEmailVerified(true);
        p.setId(UUID.randomUUID());
        p.setId(UUID.randomUUID());
        p.setOtpEnabled(false);
        when(userRepository.findByEmailAndDeletedAtIsNull("user@example.com")).thenReturn(Optional.of(p));
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(mock(UserDetails.class));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh");
        when(jwtService.getTokenRemainingTime("refresh")).thenReturn(900L);
        LoginRequest req = LoginRequest.builder().email("user@example.com").password("secret").build();
        var resp = service.login(req);
        assertThat(resp.getAccessToken()).isEqualTo("access");
    }
}
