package com.healthlink.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Bucket4j rate limiting with Redis backing.
 * Only active when healthlink.rate-limit.enabled=true.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "healthlink.rate-limit.enabled", havingValue = "true", matchIfMissing = false)
public class RateLimitConfig {

        @Value("${spring.data.redis.host:localhost}")
        private String redisHost;

        @Value("${spring.data.redis.port:6379}")
        private int redisPort;

        @Value("${spring.data.redis.password:}")
        private String redisPassword;

        @Bean
        public ProxyManager<String> proxyManager() {
                String redisUrl;
                if (redisPassword != null && !redisPassword.isEmpty()) {
                        // redis://password@host:port
                        redisUrl = "redis://" + redisPassword + "@" + redisHost + ":" + redisPort;
                } else {
                        redisUrl = "redis://" + redisHost + ":" + redisPort;
                }

                RedisClient redisClient = RedisClient.create(redisUrl);
                StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
                                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

                @SuppressWarnings("deprecation")
                var builder = LettuceBasedProxyManager.builderFor(connection)
                                .withExpirationStrategy(
                                                io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
                                                                .basedOnTimeForRefillingBucketUpToMax(
                                                                                Duration.ofMinutes(5)));
                return builder.build();
        }
}
