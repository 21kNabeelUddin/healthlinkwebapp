package com.healthlink.security.token;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AccessTokenBlacklistServiceTest {

    static class InMemoryValueOps implements ValueOperations<String, String> {
        private final Set<String> keys;

        InMemoryValueOps(Set<String> keys) {
            this.keys = keys;
        }

        @Override
        public void set(String key, String value) {
            keys.add(key);
        } // no TTL handling in test
        // Deprecated variant not used; implement no-op

        @Override
        public void set(String key, String value, long timeout) {
            keys.add(key);
        }

        @Override
        public Boolean setIfAbsent(String key, String value) {
            return keys.add(key);
        }

        // Unused methods simplified
        @Override
        public String get(Object key) {
            return keys.contains(key) ? "1" : null;
        }

        @Override
        public Long increment(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long increment(String key, long delta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Double increment(String key, double delta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer append(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAndSet(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Boolean setIfAbsent(String key, String value, long timeout, java.util.concurrent.TimeUnit unit) {
            return keys.add(key);
        }

        @Override
        public void set(String key, String value, long timeout, java.util.concurrent.TimeUnit unit) {
            keys.add(key);
        }

        // remove duplicate setIfAbsent variant
        @Override
        public void multiSet(java.util.Map<? extends String, ? extends String> map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<String> multiGet(java.util.Collection<String> keys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.springframework.data.redis.core.RedisOperations<String, String> getOperations() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Boolean setBit(String key, long offset, boolean value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Boolean getBit(String key, long offset) {
            throw new UnsupportedOperationException();
        }

        // Remove unsupported range/delete operations for current interface usage
        @Override
        public java.util.List<Long> bitField(String key,
                org.springframework.data.redis.connection.BitFieldSubCommands subCommands) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String get(String key, long start, long end) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long size(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long decrement(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long decrement(String key, long delta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAndPersist(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAndExpire(String key, java.time.Duration timeout) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAndExpire(String key, long timeout, java.util.concurrent.TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAndDelete(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(String key, String value, java.time.Duration timeout) {
            keys.add(key);
        }

        @Override
        public Boolean multiSetIfAbsent(java.util.Map<? extends String, ? extends String> map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Boolean setIfPresent(String key, String value) {
            return keys.contains(key);
        }

        @Override
        public Boolean setIfPresent(String key, String value, long timeout, java.util.concurrent.TimeUnit unit) {
            return keys.contains(key);
        }

        @Override
        public Boolean setIfPresent(String key, String value, java.time.Duration timeout) {
            return keys.contains(key);
        }

        @Override
        public String setGet(String key, String value, java.time.Duration timeout) {
            String prev = get(key);
            set(key, value, timeout);
            return prev;
        }

        @Override
        public String setGet(String key, String value, long timeout, java.util.concurrent.TimeUnit unit) {
            String prev = get(key);
            set(key, value, timeout, unit);
            return prev;
        }
    }

    static class InMemoryRedisTemplate extends RedisTemplate<String, String> {
        private final Set<String> keys = new HashSet<>();
        private final InMemoryValueOps ops = new InMemoryValueOps(keys);

        @Override
        public ValueOperations<String, String> opsForValue() {
            return ops;
        }

        @Override
        public Boolean hasKey(String key) {
            return keys.contains(key);
        }
    }

    @Test
    void blacklistAndCheck() {
        InMemoryRedisTemplate redis = new InMemoryRedisTemplate();
        AccessTokenBlacklistService service = new AccessTokenBlacklistService(redis);
        String jti = "test-jti-123";
        assertFalse(service.isBlacklisted(jti));
        service.blacklist(jti);
        assertTrue(service.isBlacklisted(jti));
    }
}