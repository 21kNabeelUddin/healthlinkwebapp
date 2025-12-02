package com.healthlink.security.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Refresh Token Service
 * Manages refresh token lifecycle: creation, rotation, revocation, and
 * blacklisting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${healthlink.jwt.refresh-token-expiration:604800000}") // 7 days default
    private long refreshTokenExpiration;

    private static final String BLACKLIST_PREFIX = "refresh:blacklist:";
    private static final int MAX_FAMILY_CHAIN_LENGTH = 10; // Prevent infinite rotation chains

    /**
     * Store new refresh token with family tracking
     */
    @Transactional
    public RefreshToken saveRefreshToken(String token, UUID userId, long ttlMillis) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(token);
        rt.setUserId(userId);
        if (rt.getFamilyId() == null) {
            rt.setFamilyId(UUID.randomUUID().toString()); // Initialize family ID for rotation tracking
        }
        rt.setExpiresAt(OffsetDateTime.now().plusSeconds(ttlMillis / 1000));

        RefreshToken saved = repository.save(rt);
        repository.flush(); // Force immediate flush to database
        log.info("Stored refresh token for user {} with familyId {} and tokenId {}", userId, saved.getFamilyId(), saved.getId());
        return saved;
    }

    /**
     * Check if token is revoked in database
     */
    public boolean isRevoked(String token) {
        return repository.findByToken(token)
                .map(RefreshToken::isRevoked)
                .orElse(true); // If not found, treat as revoked/invalid
    }

    /**
     * Revoke a refresh token and add to blacklist
     */
    public void revoke(String token) {
        repository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(OffsetDateTime.now());
            repository.save(rt);
            blacklistWithTTL(token);
            log.info("Revoked refresh token for user {}", rt.getUserId());
        });
    }

    /**
     * Rotate refresh token (replace old with new)
     * Implements refresh token rotation per OAuth 2.0 best practices
     */
    public RefreshToken rotate(RefreshToken oldToken, String newTokenString, long ttlMillis) {
        // Mark old token as revoked
        oldToken.setRevoked(true);
        oldToken.setRevokedAt(OffsetDateTime.now());
        repository.save(oldToken);
        blacklistWithTTL(oldToken.getToken());

        // Create new token in same family
        RefreshToken newToken = new RefreshToken();
        newToken.setUserId(oldToken.getUserId());
        newToken.setToken(newTokenString);
        newToken.setFamilyId(oldToken.getFamilyId()); // Maintain family
        newToken.setPreviousId(oldToken.getId());
        newToken.setExpiresAt(OffsetDateTime.now().plusSeconds(ttlMillis / 1000));

        newToken = repository.save(newToken);

        // Update old token's replacement reference
        oldToken.setReplacedById(newToken.getId());
        repository.save(oldToken);

        // Defensive check: revoke entire family if chain too long (potential token
        // theft)
        defensiveFamilyRevocation(oldToken.getFamilyId(), MAX_FAMILY_CHAIN_LENGTH);

        log.debug("Rotated refresh token for user {} in family {}", newToken.getUserId(), newToken.getFamilyId());
        return newToken;
    }

    /**
     * Defensive family revocation
     * If rotation chain exceeds threshold, revoke entire family (security measure
     * against token theft)
     */
    public void defensiveFamilyRevocation(String familyId, int maxChainLength) {
        List<RefreshToken> tokens = repository.findByFamilyIdOrderByRevokedAsc(familyId);

        if (tokens.size() > maxChainLength) {
            log.warn("Refresh token family {} exceeded max chain length {}. Revoking all tokens (possible theft).",
                    familyId, maxChainLength);

            for (RefreshToken t : tokens) {
                if (!t.isRevoked()) {
                    t.setRevoked(true);
                    t.setRevokedAt(OffsetDateTime.now());
                    repository.save(t);
                }
                blacklistWithTTL(t.getToken());
            }
        }
    }

    /**
     * Check if token is blacklisted in Redis
     * Returns false if Redis is unavailable (fault-tolerant)
     */
    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(token)));
        } catch (Exception e) {
            log.warn("Redis unavailable for blacklist check, treating as not blacklisted: {}", e.getMessage());
            return false; // If Redis is down, don't block token refresh
        }
    }

    /**
     * Add token to Redis blacklist with TTL matching refresh token expiration
     * Prevents indefinite memory growth
     */
    private void blacklistWithTTL(String token) {
        String key = blacklistKey(token);
        Duration ttl = Duration.ofMillis(refreshTokenExpiration);
        redisTemplate.opsForValue().set(key, "1", ttl);
        log.debug("Blacklisted refresh token with TTL: {}", ttl);
    }

    /**
     * Generate Redis key for blacklist
     */
    private String blacklistKey(String token) {
        return BLACKLIST_PREFIX + token;
    }

    /**
     * Revoke all refresh tokens for a user (e.g., on password change, account
     * deletion)
     */
    public void revokeAllForUser(UUID userId) {
        List<RefreshToken> tokens = repository.findByUserId(userId);
        log.info("Revoking {} refresh tokens for user {}", tokens.size(), userId);

        for (RefreshToken rt : tokens) {
            if (!rt.isRevoked()) {
                rt.setRevoked(true);
                rt.setRevokedAt(OffsetDateTime.now());
                repository.save(rt);
            }
            blacklistWithTTL(rt.getToken());
        }
    }

    /**
     * Find refresh token by token string
     */
    public java.util.Optional<RefreshToken> findByToken(String token) {
        return repository.findByToken(token);
    }

    /**
     * Clean up expired tokens (scheduled job should call this)
     */
    public int cleanupExpiredTokens() {
        List<RefreshToken> expired = repository.findByExpiresAtBefore(OffsetDateTime.now());
        int count = expired.size();

        if (count > 0) {
            repository.deleteAll(expired);
            log.info("Cleaned up {} expired refresh tokens", count);
        }

        return count;
    }
}
