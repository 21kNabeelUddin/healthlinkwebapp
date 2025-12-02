package com.healthlink.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT Service for token generation and validation
 * Handles access token (15min) and refresh token (7 days) lifecycle
 */
@Service
@Slf4j
public class JwtService {

    @Value("${healthlink.jwt.secret}")
    private String secretKey;

    @Value("${healthlink.jwt.access-token-expiration}")
    private long accessTokenExpiration; // 15 minutes = 900000ms

    @Value("${healthlink.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration; // 7 days = 604800000ms

    private static final int MINIMUM_KEY_LENGTH_BYTES = 32; // 256 bits
    private static final long CLOCK_SKEW_SECONDS = 30; // 30 second tolerance

    /**
     * Validate JWT secret key on application startup
     * Ensures key meets 256-bit minimum requirement per security best practices
     */
    @PostConstruct
    public void validateConfiguration() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT secret key must be configured: healthlink.jwt.secret");
        }

        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MINIMUM_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    String.format("JWT secret key must be at least %d bytes (256 bits). Current: %d bytes. " +
                            "Generate a secure key using: openssl rand -base64 32",
                            MINIMUM_KEY_LENGTH_BYTES, keyBytes.length));
        }

        if (accessTokenExpiration <= 0 || refreshTokenExpiration <= 0) {
            throw new IllegalStateException("JWT token expiration values must be positive");
        }

        log.info("JWT Service initialized - Access token: {}ms, Refresh token: {}ms, Key length: {} bytes",
                accessTokenExpiration, refreshTokenExpiration, keyBytes.length);
    }

    /**
     * Extract username (email) from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract username from token, even if expired (for refresh token validation)
     */
    public String extractUsernameAllowExpired(String token) {
        try {
            return extractUsername(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims().getSubject();
        }
    }

    /**
     * Validate token signature and username without checking expiration
     * Used for refresh token validation where we want to allow expired tokens within grace period
     */
    public boolean validateTokenSignature(String token, UserDetails userDetails, String expectedType) {
        try {
            String username = extractUsernameAllowExpired(token);
            
            // Validate username matches
            if (!username.equals(userDetails.getUsername())) {
                log.debug("Token username mismatch: expected={}, actual={}", userDetails.getUsername(), username);
                return false;
            }
            
            // Validate token type if specified
            if (expectedType != null) {
                String tokenType;
                try {
                    tokenType = extractTokenType(token);
                } catch (ExpiredJwtException e) {
                    tokenType = (String) e.getClaims().get("type");
                }
                if (!expectedType.equals(tokenType)) {
                    log.warn("Token type mismatch: expected={}, actual={} for user={}",
                            expectedType, tokenType, username);
                    return false;
                }
            }
            
            // If we got here, the signature is valid (extractUsernameAllowExpired validates signature)
            return true;
            
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.error("JWT signature validation failed: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract expiration date from token, even if expired
     */
    public Date extractExpirationAllowExpired(String token) {
        try {
            return extractExpiration(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims().getExpiration();
        }
    }

    /**
     * Extract user ID from token
     * 
     * @throws IllegalArgumentException if userId claim is missing or malformed
     */
    public UUID extractUserId(String token) {
        try {
            String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
            if (userIdStr == null) {
                throw new IllegalArgumentException("Token missing userId claim");
            }
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            log.error("Failed to extract userId from token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extract user role from token
     * 
     * @return role or null if not present
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extract token type (ACCESS or REFRESH)
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /**
     * Extract JTI (JWT ID) for token tracking/blacklisting
     */
    public String extractJti(String token) {
        return extractClaim(token, claims -> claims.get("jti", String.class));
    }

    /**
     * Extract single claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generate access token with user details, ID, and role
     */
    public String generateAccessToken(UserDetails userDetails, UUID userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("role", role);
        claims.put("type", "ACCESS");
        claims.put("jti", UUID.randomUUID().toString());
        return buildToken(claims, userDetails, accessTokenExpiration);
    }

    /**
     * Generate refresh token with user details and ID
     */
    public String generateRefreshToken(UserDetails userDetails, UUID userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("type", "REFRESH");
        claims.put("jti", UUID.randomUUID().toString());
        return buildToken(claims, userDetails, refreshTokenExpiration);
    }

    /**
     * Build JWT token with claims, subject, and expiration
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validate token against user details
     * Checks username match, expiration, and token type
     * 
     * @param token        JWT token to validate
     * @param userDetails  User details to validate against
     * @param expectedType Expected token type ("ACCESS" or "REFRESH"), null to skip
     *                     type check
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails, String expectedType) {
        try {
            String username = extractUsername(token);

            // Validate username matches
            if (!username.equals(userDetails.getUsername())) {
                log.debug("Token username mismatch: expected={}, actual={}", userDetails.getUsername(), username);
                return false;
            }

            // Validate not expired (with clock skew tolerance)
            if (isTokenExpired(token)) {
                log.debug("Token expired for user: {}", username);
                return false;
            }

            // Validate token type if specified
            if (expectedType != null) {
                String tokenType = extractTokenType(token);
                if (!expectedType.equals(tokenType)) {
                    log.warn("Token type mismatch: expected={}, actual={} for user={}",
                            expectedType, tokenType, username);
                    return false;
                }
            }

            return true;

        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.error("JWT signature validation failed: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token (convenience method without type checking)
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, null);
    }

    /**
     * Check if token is expired (with clock skew tolerance)
     */
    private boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        // Add clock skew tolerance
        return expiration.before(new Date(System.currentTimeMillis() - (CLOCK_SKEW_SECONDS * 1000)));
    }

    /**
     * Extract expiration date from token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .clockSkewSeconds(CLOCK_SKEW_SECONDS) // Handle clock skew between servers
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    /**
     * Get signing key from secret
     * Uses HMAC-SHA256 with 256-bit key (validated on startup)
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Get remaining time until token expiration in milliseconds
     */
    public long getTokenRemainingTime(String token) {
        Date expiration = extractExpiration(token);
        long remaining = expiration.getTime() - System.currentTimeMillis();
        return Math.max(0, remaining); // Never return negative
    }

    /**
     * Check if token is about to expire (within 1 minute)
     */
    public boolean isTokenExpiringSoon(String token) {
        return getTokenRemainingTime(token) < 60000; // 1 minute
    }
}
