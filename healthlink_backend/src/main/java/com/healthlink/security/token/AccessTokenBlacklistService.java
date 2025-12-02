package com.healthlink.security.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Access Token Blacklist Service
 * Manages blacklisted JTI (JWT IDs) for revoked access tokens
 * Uses Redis for fast lookup with TTL matching access token expiration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccessTokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${healthlink.jwt.access-token-expiration:900000}") // 15 minutes default
    private long accessTokenExpiration;

    private static final String BLACKLIST_PREFIX = "access:blacklist:jti:";

    /**
     * Add JTI to blacklist with TTL
     * Fault-tolerant: Silently fails if Redis is unavailable (MVP mode)
     * 
     * @param jti JWT ID to blacklist
     */
    public void blacklist(String jti) {
        if (jti == null || jti.isBlank()) {
            log.warn("Attempted to blacklist null or empty JTI");
            return;
        }

        try {
            String key = blacklistKey(jti);
            Duration ttl = Duration.ofMillis(accessTokenExpiration);
            redisTemplate.opsForValue().set(key, "1", ttl);
            log.debug("Blacklisted access token JTI: {} with TTL: {}", jti, ttl);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, cannot blacklist token: {}", e.getMessage());
            // Silently fail for MVP - token will expire naturally
        }
    }

    /**
     * Check if JTI is blacklisted
     * Fault-tolerant: Returns false if Redis is unavailable (MVP mode)
     * 
     * @param jti JWT ID to check
     * @return true if blacklisted, false otherwise (or if Redis unavailable)
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return true; // Treat invalid JTI as blacklisted
        }

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(jti)));
        } catch (DataAccessException e) {
            // If Redis is unavailable, treat as not blacklisted for MVP
            log.warn("Redis unavailable for blacklist check, treating as not blacklisted: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Remove JTI from blacklist (rarely needed, TTL handles expiration)
     * 
     * @param jti JWT ID to remove
     */
    public void remove(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }

        redisTemplate.delete(blacklistKey(jti));
        log.debug("Removed JTI from blacklist: {}", jti);
    }

    /**
     * Generate Redis key for JTI blacklist
     */
    private String blacklistKey(String jti) {
        return BLACKLIST_PREFIX + jti;
    }
}