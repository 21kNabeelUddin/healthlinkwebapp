package com.healthlink.service.auth;

import com.healthlink.domain.user.entity.*;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.dto.auth.*;
import com.healthlink.infrastructure.logging.SafeLogger;
import com.healthlink.security.jwt.JwtService;
import com.healthlink.security.token.RefreshTokenService;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authentication service for login, registration, and token management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final com.healthlink.security.token.AccessTokenBlacklistService accessTokenBlacklistService;
    private final com.healthlink.infrastructure.email.ApprovalEmailService approvalEmailService;

    /**
     * Register new user (multi-role support)
     */
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Email already registered");
        }

        // Validate password strength
        if (request.getPassword() != null && !isValidPassword(request.getPassword())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one number, and one special character.");
        }

        // Create user based on role
        User user = createUserByRole(request);

        // Save user
        user = userRepository.save(user);
        SafeLogger.get(AuthenticationService.class)
            .event("user_registered")
            .withMasked("email", user.getEmail())
            .with("role", user.getRole().name())
            .log();

        // For Patients and Doctors, send OTP for email verification
        // (Patients: self-serve access, Doctors: require email verification + admin approval)
        if (user.getRole() == UserRole.PATIENT || user.getRole() == UserRole.DOCTOR) {
            otpService.generateOtp(user.getEmail());
        }

        // If role requires approval (Doctor, Organization, Admin, Staff) OR email is not verified,
        // do NOT generate tokens yet. Frontend will drive OTP verification and admins will approve.
        if (requiresApproval(user.getRole()) || !Boolean.TRUE.equals(user.getIsEmailVerified())) {
            analyticsRecord(com.healthlink.analytics.AnalyticsEventType.USER_REGISTERED, user.getEmail(),
                    user.getId().toString(), "role=" + user.getRole());
            return AuthenticationResponse.builder()
                    .user(mapToUserInfo(user))
                    .build();
        }

        // Generate tokens for auto-approved AND already verified roles
        return generateAuthResponse(user);
    }

    /**
     * Login with password or OTP
     */
    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check if user can login
        if (!user.canLogin()) {
            if (user.getApprovalStatus() == ApprovalStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account pending approval");
            }
            if (user.getApprovalStatus() == ApprovalStatus.REJECTED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account has been rejected");
            }
            if (!user.getIsActive()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive");
            }
            if (!user.getIsEmailVerified()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email not verified");
            }
        }

        // New logic: password is ALWAYS required. OTP is second factor only when
        // enabled for patient.
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadCredentialsException("Password is required");
        }
        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        // If patient has OTP enabled then require valid OTP in addition to password
        if (user.getRole() == UserRole.PATIENT) {
            if (user instanceof Patient patient) {
                if (Boolean.TRUE.equals(patient.getOtpEnabled())) {
                    if (request.getOtp() == null || request.getOtp().isEmpty()) {
                        throw new BadCredentialsException("OTP required");
                    }
                    if (!otpService.verifyOtp(request.getEmail(), request.getOtp())) {
                        throw new BadCredentialsException("Invalid or expired OTP");
                    }
                }
            }
        }

        SafeLogger.get(AuthenticationService.class)
            .event("user_logged_in")
            .withMasked("email", user.getEmail())
            .with("role", user.getRole().name())
            .log();
        analyticsRecord(com.healthlink.analytics.AnalyticsEventType.USER_LOGIN, user.getEmail(),
                user.getId().toString(), "role=" + user.getRole());
        return generateAuthResponse(user);
    }

    // Request OTP (patients only)
    public String requestOtp(String email) {
        return otpService.generateOtp(email);
    }

    // Verify OTP login; auto-register patient if not exists
    @Transactional
    public AuthenticationResponse verifyOtpLogin(String email, String otp) {
        if (!otpService.verifyOtp(email, otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);
        if (user == null) {
            RegisterRequest rr = new RegisterRequest();
            rr.setEmail(email);
            rr.setRole(UserRole.PATIENT);
            rr.setFirstName("Patient");
            rr.setLastName("User");
            rr.setPreferredLanguage("en");
            user = createUserByRole(rr);
            user = userRepository.save(user);
        }
        if (!user.canLogin()) {
            throw new RuntimeException("Account not approved or inactive");
        }
        return generateAuthResponse(user);
    }

    // Forced logout on approval status change
    @Transactional
    public void updateApprovalStatus(UUID userId, ApprovalStatus status) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        ApprovalStatus previous = user.getApprovalStatus();
        user.setApprovalStatus(status);
        if (previous == ApprovalStatus.APPROVED && status != ApprovalStatus.APPROVED) {
            user.markTokensRevoked();
            refreshTokenService.revokeAllForUser(user.getId());
        }
        userRepository.save(user);
        // Send decision email if moved into a terminal state (approved / rejected) or
        // out of it
        if (status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED) {
            approvalEmailService.sendApprovalDecision(user.getId(), status);
        }
    }

    /**
     * Refresh access token using refresh token
     * Simplified MVP version: Only validates JWT signature, no database storage required
     */
    public AuthenticationResponse rotateRefreshToken(String refreshToken) {
        String userEmail;
        try {
            // Try to extract username, allowing expired tokens for refresh token validation
            userEmail = jwtService.extractUsernameAllowExpired(refreshToken);
        } catch (Exception e) {
            SafeLogger.get(AuthenticationService.class)
                    .event("refresh_token_parse_failed")
                    .with("error", e.getMessage())
                    .log();
            throw new RuntimeException("Invalid refresh token: Unable to parse token");
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(userEmail)
                .orElseThrow(() -> {
                    SafeLogger.get(AuthenticationService.class)
                            .event("refresh_token_user_not_found")
                            .withMasked("email", userEmail)
                            .log();
                    return new RuntimeException("Invalid refresh token: User not found");
                });

        // Check if user can still login
        if (!user.canLogin()) {
            SafeLogger.get(AuthenticationService.class)
                    .event("refresh_token_user_cannot_login")
                    .withMasked("email", userEmail)
                    .log();
            throw new RuntimeException("User account is not active or not approved");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        // For refresh tokens, validate signature and check expiration separately
        // Allow refresh tokens that are expired but not too old (grace period: 1 hour)
        
        // Validate signature without checking expiration
        if (!jwtService.validateTokenSignature(refreshToken, userDetails, "REFRESH")) {
            SafeLogger.get(AuthenticationService.class)
                    .event("refresh_token_jwt_invalid")
                    .withMasked("email", userEmail)
                    .log();
            throw new RuntimeException("Invalid refresh token: Signature validation failed");
        }
        
        // Check expiration separately (allow expired tokens within grace period)
        try {
            Date expiration = jwtService.extractExpirationAllowExpired(refreshToken);
            long expiredAgo = System.currentTimeMillis() - expiration.getTime();
            long gracePeriodMs = 60 * 60 * 1000; // 1 hour grace period
            
            if (expiredAgo > gracePeriodMs) {
                SafeLogger.get(AuthenticationService.class)
                        .event("refresh_token_expired_too_long")
                        .withMasked("email", userEmail)
                        .with("expiredAgoMs", expiredAgo)
                        .log();
                throw new RuntimeException("Refresh token expired. Please log in again.");
            } else if (expiredAgo > 0) {
                // Token expired but within grace period - allow refresh
                SafeLogger.get(AuthenticationService.class)
                        .event("refresh_token_expired_within_grace_period")
                        .withMasked("email", userEmail)
                        .with("expiredAgoMs", expiredAgo)
                        .log();
            }
        } catch (Exception e) {
            SafeLogger.get(AuthenticationService.class)
                    .event("refresh_token_validation_failed")
                    .withMasked("email", userEmail)
                    .with("error", e.getMessage())
                    .log();
            throw new RuntimeException("Invalid refresh token: " + e.getMessage());
        }

        // Generate new tokens (no rotation/revocation tracking for MVP)
        String newAccessToken = jwtService.generateAccessToken(userDetails, user.getId(), user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(userDetails, user.getId());
        
        SafeLogger.get(AuthenticationService.class)
                .event("refresh_token_success")
                .withMasked("email", userEmail)
                .log();
        
        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getTokenRemainingTime(newAccessToken))
                .user(mapToUserInfo(user))
                .build();
    }

    /**
     * Initiate password reset for Patient and Doctor roles.
     * Generates an OTP and sends it via email (re-uses existing OTP flow).
     * For security, this method is silent even if the user/email does not exist.
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(user -> {
            if (user.getRole() != UserRole.PATIENT && user.getRole() != UserRole.DOCTOR) {
                // Only patients and doctors are allowed to reset passwords via this flow
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Password reset is only available for Patient and Doctor accounts.");
            }
            otpService.generateOtp(email);
            SafeLogger.get(AuthenticationService.class)
                    .event("password_reset_otp_sent")
                    .withMasked("email", email)
                    .log();
        });
        // Always return success message from controller to avoid user enumeration.
    }

    /**
     * Complete password reset using email + OTP + new password.
     */
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email or OTP"));

        if (user.getRole() != UserRole.PATIENT && user.getRole() != UserRole.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password reset is only available for Patient and Doctor accounts.");
        }

        if (!otpService.verifyOtp(email, otp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }

        if (!isValidPassword(newPassword)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one number, and one special character.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.markTokensRevoked();
        refreshTokenService.revokeAllForUser(user.getId());
        userRepository.save(user);

        SafeLogger.get(AuthenticationService.class)
                .event("password_reset_success")
                .withMasked("email", email)
                .log();
    }

    /**
     * Simplified MVP: No refresh token revocation needed since we don't store them
     */
    public void revokeRefreshToken(String refreshToken) {
        // MVP: No-op since we don't store refresh tokens
        SafeLogger.get(AuthenticationService.class)
                .event("refresh_token_revoke_skipped")
                .log();
    }

    /**
     * Logout current session: blacklist access token JTI
     * Simplified MVP: Refresh tokens are stateless, so no revocation needed
     */
    public void logout(String accessToken, String refreshToken) {
        String jti = jwtService.extractJti(accessToken);
        accessTokenBlacklistService.blacklist(jti);
        // MVP: Refresh tokens are stateless, no revocation needed
        SafeLogger.get(AuthenticationService.class)
                .event("user_logged_out")
                .with("accessTokenJti", jti)
                .log();
    }

    /**
     * Create user entity based on role
     */
    private User createUserByRole(RegisterRequest request) {
        User user;

        switch (request.getRole()) {
            case PATIENT -> {
                Patient patient = new Patient();
                // Set patient-specific fields
                if (request.getDateOfBirth() != null && !request.getDateOfBirth().isEmpty()) {
                    try {
                        // Parse ISO date string (e.g., "2025-11-05T00:00:00" or "2025-11-05")
                        String dateStr = request.getDateOfBirth();
                        if (dateStr.contains("T")) {
                            dateStr = dateStr.substring(0, dateStr.indexOf("T"));
                        }
                        patient.setDateOfBirth(java.time.LocalDate.parse(dateStr));
                    } catch (Exception e) {
                        // Invalid date format - ignore and continue without dateOfBirth
                    }
                }
                if (request.getAddress() != null && !request.getAddress().isEmpty()) {
                    patient.setAddress(request.getAddress());
                }
                user = patient;
                // Auto-approve patients
                user.setApprovalStatus(ApprovalStatus.APPROVED);
                user.setIsEmailVerified(false); // Require email verification via OTP
                // Patients use OTP-based authentication (no password required per spec)
            }
            case DOCTOR -> {
                Doctor doctor = new Doctor();
                doctor.setPmdcId(request.getPmdcId());
                doctor.setSpecialization(request.getSpecialization());
                doctor.setPmdcVerified(false);
                user = doctor;
                // Doctors require approval
                user.setApprovalStatus(ApprovalStatus.PENDING);
            }
            case STAFF -> {
                Staff staff = new Staff();
                user = staff;
                user.setApprovalStatus(ApprovalStatus.PENDING);
            }
            case ORGANIZATION -> {
                Organization org = new Organization();
                org.setOrganizationName(request.getOrganizationName());
                org.setPakistanOrgNumber(request.getPakistanOrgNumber());
                org.setOrgVerified(false);
                user = org;
                user.setApprovalStatus(ApprovalStatus.PENDING);
            }
            case ADMIN -> {
                Admin admin = new Admin();
                admin.setAdminUsername(request.getAdminUsername());
                user = admin;
                user.setApprovalStatus(ApprovalStatus.PENDING);
            }
            case PLATFORM_OWNER -> {
                PlatformOwner owner = new PlatformOwner();
                owner.setOwnerUsername(request.getOwnerUsername());
                user = owner;
                user.setApprovalStatus(ApprovalStatus.APPROVED); // Auto-approve
            }
            default -> throw new RuntimeException("Invalid user role");
        }

        // Set common fields
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole());
        user.setPreferredLanguage(request.getPreferredLanguage());
        user.setIsActive(true);
        // Require email verification for all users
        user.setIsEmailVerified(false);

        // Hash password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return user;
    }

    /**
     * Toggle OTP requirement for the authenticated patient.
     */
    @Transactional
    public void togglePatientOtp(boolean enabled) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Unauthenticated");
        }
        User user = userRepository.findByEmailAndDeletedAtIsNull(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != UserRole.PATIENT) {
            throw new RuntimeException("Only patients can toggle OTP");
        }
        if (!(user instanceof Patient patient)) {
            throw new RuntimeException("Invalid user type");
        }
        patient.setOtpEnabled(enabled);
        userRepository.save(patient);
        SafeLogger.get(AuthenticationService.class)
            .event("patient_otp_toggled")
            .withMasked("email", patient.getEmail())
            .with("enabled", enabled)
            .log();
    }

    /**
     * Generate authentication response with tokens
     * Simplified MVP version: No database storage of refresh tokens
     */
    private AuthenticationResponse generateAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtService.generateAccessToken(userDetails, user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(userDetails, user.getId());
        
        // Simplified MVP: Just generate tokens, no database storage
        SafeLogger.get(AuthenticationService.class)
                .event("tokens_generated")
                .withMasked("email", user.getEmail())
                .log();
        
        analyticsRecord(com.healthlink.analytics.AnalyticsEventType.USER_LOGIN, user.getEmail(),
                user.getId().toString(), "issueTokens");
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(14400000L) // 4 hours
                .user(mapToUserInfo(user))
                .build();
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.healthlink.analytics.AnalyticsEventService analyticsEventService;

    private void analyticsRecord(com.healthlink.analytics.AnalyticsEventType type, String actor, String subjectId,
            String meta) {
        if (analyticsEventService != null) {
            analyticsEventService.record(type, actor, subjectId, meta);
        }
    }

    /**
     * Map User entity to UserInfoDto
     */
    private UserInfoDto mapToUserInfo(User user) {
        return UserInfoDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole())
                .approvalStatus(user.getApprovalStatus())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .profilePictureUrl(user.getProfilePictureUrl())
                .preferredLanguage(user.getPreferredLanguage())
                .build();
    }

    /**
     * Check if role requires approval
     */
    private boolean requiresApproval(UserRole role) {
        return role == UserRole.DOCTOR || role == UserRole.ORGANIZATION ||
                role == UserRole.ADMIN || role == UserRole.STAFF;
    }

    /**
     * Mark user email as verified
     */
    @Transactional
    public void markEmailVerified(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Mark email as verified
        user.setIsEmailVerified(true);

        // MVP: Auto-approve doctors once their email is verified so they can log in
        // (In a full production system this would stay PENDING until an admin reviews)
        if (user.getRole() == UserRole.DOCTOR && user.getApprovalStatus() == ApprovalStatus.PENDING) {
            user.setApprovalStatus(ApprovalStatus.APPROVED);
        }

        userRepository.save(user);

        SafeLogger.get(AuthenticationService.class)
            .event("email_verified")
            .withMasked("email", email)
            .log();
    }

    /**
     * Validate password strength
     */
    private boolean isValidPassword(String password) {
        // At least 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        return password.matches(regex);
    }
}
