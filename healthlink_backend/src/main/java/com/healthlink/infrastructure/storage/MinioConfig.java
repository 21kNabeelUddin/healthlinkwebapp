package com.healthlink.infrastructure.storage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO configuration providing a MinioClient bean.
 * Uses properties under healthlink.storage.* defined in application.yml.
 */
@Configuration
@ConditionalOnProperty(prefix = "healthlink.storage", name = "enabled", havingValue = "true")
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${healthlink.storage.endpoint}") String endpoint,
            @Value("${healthlink.storage.access-key}") String accessKey,
            @Value("${healthlink.storage.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
