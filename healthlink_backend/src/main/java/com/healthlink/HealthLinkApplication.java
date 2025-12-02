package com.healthlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

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
        org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration.class,
        // Exclude Elasticsearch auto-configuration for MVP (optional dependency)
        org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration.class
})
// Caching disabled for MVP (removed @EnableCaching to avoid Redis dependency)
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class HealthLinkApplication {

    public static void main(String[] args) {
        // Load .env file if it exists (for local development)
        try {
            // Try multiple locations for .env file
            // Note: When running via Gradle bootRun, working directory is the project root (healthlink_backend/)
            String currentDir = System.getProperty("user.dir");
            System.out.println("Working directory detected: " + currentDir);
            
            // Determine candidate directories that might contain the .env file
            File workingDirectory = new File(currentDir).getAbsoluteFile();
            File parentDirectory = workingDirectory.getParentFile();
            
            File[] candidateDirs = new File[] {
                    workingDirectory,
                    parentDirectory != null ? new File(parentDirectory, "healthlink_backend") : null,
                    parentDirectory,
                    new File(".") // fallback to whatever the process considers "."
            };
            
            File envFile = null;
            for (File dir : candidateDirs) {
                if (dir == null) {
                    continue;
                }
                File candidate = new File(dir, ".env");
                if (candidate.exists() && candidate.isFile()) {
                    envFile = candidate.getAbsoluteFile();
                    break;
                }
            }
            
            Map<String, String> envEntries = envFile != null ? readEnvFile(envFile) : Map.of();
            String foundPath = envFile != null ? envFile.getAbsolutePath() : null;
            
            // Debug: Print what we found
            int totalEntries = envEntries.size();
            if (!envEntries.isEmpty()) {
                System.out.println("Debug: Found .env with " + totalEntries + " total entries at: " + foundPath);
            }
            
            if (!envEntries.isEmpty()) {
                // Set system properties from .env file so Spring Boot can use them
                int count = 0;
                int emptyCount = 0;
                String datasourceUrlValue = null;
                
                for (var entry : envEntries.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    
                    // Debug: Check SPRING_DATASOURCE_URL specifically
                    if (key.equals("SPRING_DATASOURCE_URL")) {
                        System.out.println("Debug: Found SPRING_DATASOURCE_URL");
                        System.out.println("  Value: " + (value != null ? (value.length() > 50 ? value.substring(0, 50) + "..." : value) : "null"));
                        datasourceUrlValue = value;
                    }
                    
                    // Skip empty values
                    if (value == null || value.trim().isEmpty()) {
                        emptyCount++;
                        continue;
                    }
                    
                    // Always set (overrides existing system properties/environment for current process)
                    System.setProperty(key, value);
                    count++;
                    
                    // Also set Spring Boot relaxed binding properties when needed
                    if (key.equals("SPRING_DATASOURCE_URL")) {
                        System.setProperty("spring.datasource.url", value);
                        String maskedUrl = value.length() > 50 ? value.substring(0, 50) + "..." : value;
                        System.out.println("  ✓ Setting " + key + " = " + maskedUrl);
                    } else if (key.equals("SPRING_DATASOURCE_USERNAME")) {
                        System.setProperty("spring.datasource.username", value);
                    } else if (key.equals("SPRING_DATASOURCE_PASSWORD")) {
                        System.setProperty("spring.datasource.password", value);
                    }
                }
                if (count > 0) {
                    System.out.println("✓ Loaded " + count + " environment variables from .env file" + 
                        (foundPath != null ? " (found at: " + foundPath + ")" : ""));
                    
                    // Verify the property was set
                    String dbUrl = System.getProperty("SPRING_DATASOURCE_URL");
                    if (dbUrl != null && !dbUrl.contains("postgres:")) {
                        System.out.println("✓ Database URL configured: " + dbUrl.substring(0, Math.min(60, dbUrl.length())) + "...");
                    } else if (dbUrl != null) {
                        System.out.println("⚠ Warning: Database URL still points to 'postgres' hostname");
                    } else {
                        System.out.println("⚠ Warning: SPRING_DATASOURCE_URL not found in .env file");
                    }
                } else {
                    System.out.println("⚠ Warning: .env file found but no variables were loaded");
                    System.out.println("  Total entries found: " + totalEntries);
                    System.out.println("  Empty values: " + emptyCount);
                    if (datasourceUrlValue != null && !datasourceUrlValue.trim().isEmpty()) {
                        System.out.println("  SPRING_DATASOURCE_URL value exists but was not set - forcing it now");
                        System.setProperty("SPRING_DATASOURCE_URL", datasourceUrlValue);
                        System.setProperty("spring.datasource.url", datasourceUrlValue);
                        System.out.println("  ✓ Force-set SPRING_DATASOURCE_URL");
                    }
                }
            } else if (envFile != null) {
                System.out.println("⚠ Warning: .env file found at " + envFile.getAbsolutePath() + " but it could not be parsed.");
            } else {
                System.out.println("Note: .env file not found. Using system environment variables.");
            }
        } catch (Exception e) {
            // .env file not found or error loading - continue without it
            System.out.println("Note: .env file not found. Using system environment variables.");
        }
        
        SpringApplication.run(HealthLinkApplication.class, args);
    }

    private static Map<String, String> readEnvFile(File envFile) {
        Map<String, String> entries = new LinkedHashMap<>();
        try {
            for (String rawLine : Files.readAllLines(envFile.toPath(), StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx < 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                entries.put(key, value);
            }
        } catch (IOException e) {
            System.out.println("Warning: Unable to read .env file (" + envFile.getAbsolutePath() + "): " + e.getMessage());
        }
        return entries;
    }
}
