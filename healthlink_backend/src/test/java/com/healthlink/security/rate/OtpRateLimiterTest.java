package com.healthlink.security.rate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OtpRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String,String> valueOps;
    private OtpRateLimiter rateLimiter;

    @BeforeEach
    void setup() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String,String> ops = Mockito.mock(ValueOperations.class);
        valueOps = ops;
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        rateLimiter = new OtpRateLimiter(redisTemplate);
    }

    @Test
    void allowsFirstRequest() {
        when(valueOps.increment(anyString())).thenReturn(1L);
        var result = rateLimiter.consume("patient@example.com");
        assertTrue(result.allowed());
    }

    @Test
    void blocksAfterLimitExceeded() {
        when(valueOps.increment(anyString())).thenReturn(6L);
        var result = rateLimiter.consume("patient@example.com");
        assertFalse(result.allowed());
    }
}
