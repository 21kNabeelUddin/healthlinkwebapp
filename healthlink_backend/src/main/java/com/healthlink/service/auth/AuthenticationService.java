package com.healthlink.service.auth;

import com.healthlink.domain.user.entity.*;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.dto.auth.*;
import com.healthlink.infrastructure.logging.SafeLogger;
import com.healthlink.security.jwt.JwtService;
import com.healthlink.security.token.RefreshTokenService;
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

        // For roles requiring approval, don't generate tokens
        if (requiresApproval(user.getRole())) {
            analyticsRecord(com.healthlink.analytics.AnalyticsEventType.USER_REGISTERED, user.getEmail(),
                    user.getId().toString(), "role=" + user.getRole());
            return AuthenticationResponse.builder()
                    .user(mapToUserInfo(user))
                    .build();
        }

        // For Patients, require Email Verification via OTP before issuing tokens
        if (user.getRole() == UserRole.PATIENT) {
            otpService.generateOtp(user.getEmail()); // Send OTP (simulated/email)
            // In a real app, we'd trigger an email event here.
            // For now, we assume OtpService handles generation.
            // We do NOT return tokens.
            return AuthenticationResponse.builder()
                    .user(mapToUserInfo(user))
                    .build(); // No tokens
        }

        // For Platform Owner (auto-approved, maybe no OTP enforced for dev?), generate
        // tokens
        // But strictly, spec says "Password-based login... with 2fa support".
        // If Platform Owner needs verification, we should enforce it.
        // Assuming Platform Owner is trusted/seeded, we might allow auto-login or
        // enforce same flow.
        // For safety, let's enforce verification for everyone if isEmailVerified is
        // false.

        if (!user.getIsEmailVerified()) {
            // If not verified, don't issue tokens.
            return AuthenticationResponse.builder()
                    .user(mapToUserInfo(user))
                    .build();
        }

        // Generate tokens for auto-approved AND verified roles
        return generateAuthResponse(user);
    }

    /**
     * Login with password or OTP
     */
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
     */
    public AuthenticationResponse rotateRefreshToken(String refreshToken) {
        final String userEmail = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByEmailAndDeletedAtIsNull(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        if (!jwtService.isTokenValid(refreshToken, userDetails) || refreshTokenService.isRevoked(refreshToken)
                || refreshTokenService.isBlacklisted(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        var oldToken = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        // Defensive: if chain seems abnormally long, revoke family (threshold 25)
        refreshTokenService.defensiveFamilyRevocation(oldToken.getFamilyId(), 25);
        String newAccessToken = jwtService.generateAccessToken(userDetails, user.getId(), user.getRole().name());
        String newRefreshTokenValue = jwtService.generateRefreshToken(userDetails, user.getId());
        var rotated = refreshTokenService.rotate(oldToken, newRefreshTokenValue,
                jwtService.getTokenRemainingTime(newRefreshTokenValue));
        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(rotated.getToken())
                .tokenType("Bearer")
                .expiresIn(900000L)
                .user(mapToUserInfo(user))
                .build();
    }

    public void revokeRefreshToken(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    /**
     * Logout current session: blacklist access token JTI and revoke provided
     * refresh token.
     */
    public void logout(String accessToken, String refreshToken) {
        String jti = jwtService.extractJti(accessToken);
        accessTokenBlacklistService.blacklist(jti);
        if (refreshToken != null && !refreshToken.isEmpty()) {
            revokeRefreshToken(refreshToken);
        }
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
     */
    private AuthenticationResponse generateAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtService.generateAccessToken(userDetails, user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(userDetails, user.getId());
        refreshTokenService.saveRefreshToken(refreshToken, user.getId(),
                jwtService.getTokenRemainingTime(refreshToken));
        analyticsRecord(com.healthlink.analytics.AnalyticsEventType.USER_LOGIN, user.getEmail(),
                user.getId().toString(), "issueTokens");
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900000L)
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
        user.setIsEmailVerified(true);
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
