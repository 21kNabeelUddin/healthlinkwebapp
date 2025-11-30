package com.healthlink.security.token;

import com.healthlink.infrastructure.logging.SafeLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private final StringRedisTemplate redisTemplate;
    private final SafeLogger log = SafeLogger.get(TokenBlacklistService.class);

    public void blacklist(String accessToken) {
        // Store token string with TTL equal to remaining validity (approximate 15 min window)
        redisTemplate.opsForValue().set(buildKey(accessToken), "1", Duration.ofMinutes(16));
        log.event("access_token_blacklisted").log();
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(accessToken)));
    }

    private String buildKey(String token) {
        return "blacklist:" + Integer.toHexString(token.hashCode());
    }
}
