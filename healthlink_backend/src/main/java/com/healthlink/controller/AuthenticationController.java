package com.healthlink.controller;

import com.healthlink.dto.auth.*;
import com.healthlink.service.auth.AuthenticationService;
import com.healthlink.service.auth.OtpService;
import com.healthlink.security.rate.OtpRateLimiter;
import com.healthlink.infrastructure.logging.SafeLogger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Authentication REST Controller
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and token management")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final OtpService otpService;
    private final OtpRateLimiter otpRateLimiter;

    private final SafeLogger safeLogger = SafeLogger.get(AuthenticationController.class);

    /**
     * Register new user
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Registers a new user (Patient, Doctor, etc.). Patients require email verification.")
    @ApiResponse(responseCode = "200", description = "Registration successful", content = @Content(schema = @Schema(implementation = AuthenticationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        // Use SafeLogger to avoid logging raw PHI (email)
        safeLogger.event("registration_request")
                .withMasked("email", request.getEmail())
                .log();
        AuthenticationResponse response = authenticationService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Login with password or OTP
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate using email and password (and OTP if enabled for patients).")
    @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthenticationResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request) {
        safeLogger.event("login_request")
                .withMasked("email", request.getEmail())
                .log();
        AuthenticationResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Send OTP to email
     */
    @PostMapping("/otp/send")
    @Operation(summary = "Send OTP", description = "Send a 6-digit OTP to the specified email.")
    @ApiResponse(responseCode = "200", description = "OTP sent successfully")
    @ApiResponse(responseCode = "429", description = "Too many requests")
    public ResponseEntity<Object> sendOtp(@Valid @RequestBody OtpRequest request) {
        safeLogger.event("otp_request")
                .withMasked("email", request.getEmail())
                .log();
        var limitResult = otpRateLimiter.consume(request.getEmail());
        if (!limitResult.isAllowed()) {
            return ResponseEntity.status(429).body(limitResult); // JSON DTO with retry info
        }
        String otp = otpService.generateOtp(request.getEmail());
        return ResponseEntity.ok("OTP sent successfully to " + request.getEmail() + " (DEV: " + otp + ")");
    }

    /**
     * Verify email with OTP
     */
    @PostMapping("/email/verify")
    @Operation(summary = "Verify Email", description = "Verify email address using the received OTP.")
    @ApiResponse(responseCode = "200", description = "Email verified")
    @ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    public ResponseEntity<String> verifyEmail(@Valid @RequestBody OtpRequest request) {
        if (!otpService.verifyOtp(request.getEmail(), request.getOtp())) {
            return ResponseEntity.badRequest().body("Invalid or expired OTP");
        }
        // Mark email as verified
        authenticationService.markEmailVerified(request.getEmail());
        return ResponseEntity.ok("Email verified successfully");
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "Get a new access token using a valid refresh token.")
    @ApiResponse(responseCode = "200", description = "Token refreshed", content = @Content(schema = @Schema(implementation = AuthenticationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid refresh token")
    public ResponseEntity<AuthenticationResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        String refreshToken = authHeader.substring(7);
        AuthenticationResponse response = authenticationService.rotateRefreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout (client-side token deletion + server-side revocation)
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate access token and optionally revoke refresh token.")
    @ApiResponse(responseCode = "204", description = "Logged out successfully")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshTokenHeader,
            @RequestBody(required = false) LogoutRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        String accessToken = authHeader.substring(7);
        // Support refresh token from header or request body
        String refreshToken = refreshTokenHeader != null ? refreshTokenHeader 
                : (request != null ? request.getRefreshToken() : null);

        authenticationService.logout(accessToken, refreshToken);

        return ResponseEntity.noContent().build();
    }
}
