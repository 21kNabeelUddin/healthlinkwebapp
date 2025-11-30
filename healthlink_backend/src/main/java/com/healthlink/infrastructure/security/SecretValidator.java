package com.healthlink.infrastructure.security;

import com.healthlink.infrastructure.logging.SafeLogger;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class SecretValidator {

    private static final SafeLogger log = SafeLogger.getLogger(SecretValidator.class);

    @Value("${healthlink.jwt.secret}")
    private String jwtSecret;
    @Value("${spring.datasource.password:changeme}")
    private String dbPassword;
    @Value("${healthlink.storage.access-key:missing}")
    private String minioAccess;
    @Value("${healthlink.storage.secret-key:missing}")
    private String minioSecret;
    @Value("${spring.mail.host:missing}")
    private String mailHost;
    @Value("${spring.data.redis.host:missing}")
    private String redisHost;

    @PostConstruct
    public void validateSecrets() {
        List<String> criticalIssues = new ArrayList<>();
        if ("your-256-bit-secret-key-change-this-in-production-use-strong-random-key".equals(jwtSecret)) {
            criticalIssues.add("Default JWT secret in use");
        }
        if ("changeme".equals(dbPassword)) {
            criticalIssues.add("Default DB password in use");
        }
        if ("missing".equals(minioAccess) || "missing".equals(minioSecret)) {
            criticalIssues.add("MinIO credentials not configured");
        }
        if ("missing".equals(mailHost)) {
            criticalIssues.add("Mail host not configured");
        }
        if ("missing".equals(redisHost)) {
            criticalIssues.add("Redis host not configured");
        }
        if (criticalIssues.isEmpty()) {
            log.info("SecretValidator: All critical secrets present.");
        } else {
            log.warn("SECURITY MISCONFIGURATION: {}", String.join("; ", criticalIssues));
        }
    }
}
