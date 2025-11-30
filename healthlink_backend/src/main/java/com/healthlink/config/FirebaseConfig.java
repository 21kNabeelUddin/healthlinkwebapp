package com.healthlink.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

/**
 * Firebase Configuration
 * Configures Firebase Admin SDK for push notifications
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Value("${healthlink.firebase.credentials-file:}")
    private String firebaseCredentialsPath;

    @Value("${healthlink.firebase.project-id:}")
    private String firebaseProjectId;

    @PostConstruct
    public void initialize() {
        try {
            Resource credentialsResource = resolveCredentialsResource();
            if (credentialsResource == null || firebaseProjectId == null || firebaseProjectId.isBlank()) {
                log.warn("Firebase credentials missing. Push notifications will not initialize.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsResource.getInputStream()))
                    .setProjectId(firebaseProjectId)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully for project: {}", firebaseProjectId);
            } else {
                log.info("Firebase Admin SDK already initialized");
            }

        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    private Resource resolveCredentialsResource() {
        if (firebaseCredentialsPath == null || firebaseCredentialsPath.isBlank()) {
            return null;
        }
        Resource resource = resourceLoader.getResource(firebaseCredentialsPath);
        return resource.exists() ? resource : null;
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. FirebaseMessaging bean will not be functional.");
            return null;
        }
        return FirebaseMessaging.getInstance();
    }
}
