package com.healthlink.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for JwtService
 * Covers token generation, validation, claim extraction, and edge cases
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUser;
    private UUID testUserId;
    private String testRole;
    private String validSecret;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Valid 256-bit secret key
        validSecret = "test-secret-key-with-at-least-32-characters-for-256-bit-security";
        ReflectionTestUtils.setField(jwtService, "secretKey", validSecret);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900000L); // 15 min
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L); // 7 days

        jwtService.validateConfiguration();

        testUser = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        testUserId = UUID.randomUUID();
        testRole = "PATIENT";
    }

    @Test
    void validateConfiguration_shouldPassWithValidSecret() {
        // Should not throw - setUp already calls this
        assertThatCode(() -> jwtService.validateConfiguration()).doesNotThrowAnyException();
    }

    @Test
    void validateConfiguration_shouldFailWithShortSecret() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", "short");
        ReflectionTestUtils.setField(service, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(service, "refreshTokenExpiration", 604800000L);

        assertThatThrownBy(() -> service.validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void validateConfiguration_shouldFailWithNullSecret() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", null);
        ReflectionTestUtils.setField(service, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(service, "refreshTokenExpiration", 604800000L);

        assertThatThrownBy(() -> service.validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured");
    }

    @Test
    void generateAccessToken_shouldCreateValidToken() {
        String token = jwtService.generateAccessToken(testUser, testUserId, testRole);

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUserId);
        assertThat(jwtService.extractRole(token)).isEqualTo(testRole);
        assertThat(jwtService.extractTokenType(token)).isEqualTo("ACCESS");
        assertThat(jwtService.extractJti(token)).isNotNull();
    }

    @Test
    void generateRefreshToken_shouldCreateValidToken() {
        String token = jwtService.generateRefreshToken(testUser, testUserId);

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUserId);
        assertThat(jwtService.extractTokenType(token)).isEqualTo("REFRESH");
        assertThat(jwtService.extractJti(token)).isNotNull();
        // Refresh token shouldn't have role
        assertThat(jwtService.extractRole(token)).isNull();
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        String token = jwtService.generateAccessToken(testUser, testUserId, testRole);

        boolean valid = jwtService.isTokenValid(token, testUser);

        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValid_shouldValidateTokenType() {
        String accessToken = jwtService.generateAccessToken(testUser, testUserId, testRole);
        String refreshToken = jwtService.generateRefreshToken(testUser, testUserId);

        assertThat(jwtService.isTokenValid(accessToken, testUser, "ACCESS")).isTrue();
        assertThat(jwtService.isTokenValid(accessToken, testUser, "REFRESH")).isFalse();

        assertThat(jwtService.isTokenValid(refreshToken, testUser, "REFRESH")).isTrue();
        assertThat(jwtService.isTokenValid(refreshToken, testUser, "ACCESS")).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForWrongUsername() {
        String token = jwtService.generateAccessToken(testUser, testUserId, testRole);

        UserDetails wrongUser = User.builder()
                .username("wrong@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        boolean valid = jwtService.isTokenValid(token, wrongUser);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() {
        // Create a token that's already expired (more than 30s clock skew tolerance)
        // by using reflection to create a token with past expiration
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .subject(testUser.getUsername())
                .claim("userId", testUserId.toString())
                .claim("role", testRole)
                .issuedAt(new Date(System.currentTimeMillis() - 120_000)) // 2 minutes ago
                .expiration(new Date(System.currentTimeMillis() - 60_000)) // 1 minute ago (past clock skew)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        validSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        boolean valid = jwtService.isTokenValid(expiredToken, testUser);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForMalformedToken() {
        String malformedToken = "not.a.valid.jwt.token";

        boolean valid = jwtService.isTokenValid(malformedToken, testUser);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForTokenWithWrongSignature() {
        // Create token with different secret
        SecretKey wrongKey = Keys
                .hmacShaKeyFor("different-secret-key-with-32-chars-minimum".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(testUser.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900000))
                .signWith(wrongKey, Jwts.SIG.HS256)
                .compact();

        boolean valid = jwtService.isTokenValid(token, testUser);

        assertThat(valid).isFalse();
    }

    @Test
    void extractUserId_shouldThrowForMissingClaim() {
        // Create token without userId claim
        SecretKey key = Keys.hmacShaKeyFor(validSecret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(testUser.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.extractUserId(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void extractUserId_shouldThrowForMalformedUUID() {
        SecretKey key = Keys.hmacShaKeyFor(validSecret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(testUser.getUsername())
                .claim("userId", "not-a-valid-uuid")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.extractUserId(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractRole_shouldReturnNullForMissingClaim() {
        String token = jwtService.generateRefreshToken(testUser, testUserId);

        String role = jwtService.extractRole(token);

        assertThat(role).isNull();
    }

    @Test
    void getTokenRemainingTime_shouldReturnPositiveForValidToken() {
        String token = jwtService.generateAccessToken(testUser, testUserId, testRole);

        long remaining = jwtService.getTokenRemainingTime(token);

        assertThat(remaining).isGreaterThan(0).isLessThanOrEqualTo(900000L);
    }

    @Test
    void getTokenRemainingTime_shouldReturnZeroForExpiredToken() {
        JwtService shortExpiryService = new JwtService();
        ReflectionTestUtils.setField(shortExpiryService, "secretKey", validSecret);
        ReflectionTestUtils.setField(shortExpiryService, "accessTokenExpiration", 1L);
        ReflectionTestUtils.setField(shortExpiryService, "refreshTokenExpiration", 1L);
        shortExpiryService.validateConfiguration();

        String token = shortExpiryService.generateAccessToken(testUser, testUserId, testRole);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long remaining = jwtService.getTokenRemainingTime(token);

        assertThat(remaining).isZero();
    }

    @Test
    void isTokenExpiringSoon_shouldReturnTrueForSoonExpiringToken() {
        JwtService shortExpiryService = new JwtService();
        ReflectionTestUtils.setField(shortExpiryService, "secretKey", validSecret);
        ReflectionTestUtils.setField(shortExpiryService, "accessTokenExpiration", 30000L); // 30 seconds
        ReflectionTestUtils.setField(shortExpiryService, "refreshTokenExpiration", 30000L);
        shortExpiryService.validateConfiguration();

        String token = shortExpiryService.generateAccessToken(testUser, testUserId, testRole);

        boolean expiringSoon = shortExpiryService.isTokenExpiringSoon(token);

        assertThat(expiringSoon).isTrue();
    }

    @Test
    void isTokenExpiringSoon_shouldReturnFalseForFreshToken() {
        String token = jwtService.generateAccessToken(testUser, testUserId, testRole);

        boolean expiringSoon = jwtService.isTokenExpiringSoon(token);

        assertThat(expiringSoon).isFalse();
    }

    @Test
    void extractJti_shouldReturnUniqueId() {
        String token1 = jwtService.generateAccessToken(testUser, testUserId, testRole);
        String token2 = jwtService.generateAccessToken(testUser, testUserId, testRole);

        String jti1 = jwtService.extractJti(token1);
        String jti2 = jwtService.extractJti(token2);

        assertThat(jti1).isNotNull().isNotEmpty();
        assertThat(jti2).isNotNull().isNotEmpty();
        assertThat(jti1).isNotEqualTo(jti2); // Each token has unique JTI
    }

    @Test
    void extractClaim_shouldExtractCustomClaim() {
        String token = jwtService.generateAccessToken(testUser, testUserId, testRole);

        Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);

        assertThat(issuedAt).isNotNull().isBeforeOrEqualTo(new Date());
    }
}
