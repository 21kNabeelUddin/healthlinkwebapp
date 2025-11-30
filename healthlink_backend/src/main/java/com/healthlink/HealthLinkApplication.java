package com.healthlink;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * HealthLink Backend Application
 * 
 * A comprehensive healthcare SaaS platform supporting:
 * - Multi-role workflows (Patient, Doctor, Staff, Organization, Admin, Platform
 * Owner)
 * - Appointment management with smart scheduling
 * - Video consultations via WebRTC
 * - Manual payment verification with dispute resolution
 * - HIPAA-compliant medical record management
 * - PHI protection with comprehensive audit logging
 * 
 * Note: JPA Auditing is configured in JpaConfig.java with auditorAwareRef
 * 
 * @author HealthLink Team
 * @since 1.0.0
 */
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration.class
})
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class HealthLinkApplication {

    public static void main(String[] args) {
        // Load .env file if it exists (for local development)
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing()
                    .load();
            
            // Set system properties from .env file so Spring Boot can use them
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                // Only set if not already set as system property or environment variable
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            });
            System.out.println("✓ Loaded environment variables from .env file");
        } catch (Exception e) {
            // .env file not found or error loading - continue without it
            System.out.println("Note: .env file not found. Using system environment variables.");
        }
        
        SpringApplication.run(HealthLinkApplication.class, args);
    }
}
