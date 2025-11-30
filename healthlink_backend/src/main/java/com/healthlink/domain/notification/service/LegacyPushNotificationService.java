package com.healthlink.domain.notification.service;

import com.healthlink.domain.notification.entity.PushDeviceToken;
import com.healthlink.domain.notification.repository.PushDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Legacy Push Notification Service using FCM HTTP v1 API.
 * 
 * @deprecated Use {@link com.healthlink.service.notification.PushNotificationService} instead.
 *             This service uses the legacy FCM HTTP API which is deprecated by Firebase.
 *             The new service uses Firebase Admin SDK with proper error handling and token management.
 */
@Deprecated(since = "1.0.0", forRemoval = true)
@Service("legacyPushNotificationService")
@RequiredArgsConstructor
@Slf4j
public class LegacyPushNotificationService {

    private final PushDeviceTokenRepository tokenRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${fcm.server.key:}")
    private String fcmServerKey;

    public void registerToken(UUID userId, String token, String platform) {
        var existing = tokenRepository.findByToken(token).orElse(null);
        if (existing != null) {
            existing.setUserId(userId); // Re-link if needed
            existing.setPlatform(platform);
            existing.setLastSeenAt(Instant.now());
            tokenRepository.save(existing);
            return;
        }
        PushDeviceToken dt = new PushDeviceToken();
        dt.setUserId(userId);
        dt.setToken(token);
        dt.setPlatform(platform);
        dt.setLastSeenAt(Instant.now());
        tokenRepository.save(dt);
        log.info("Registered push token for user {}", userId);
    }

    public void sendToUser(UUID userId, String title, String body) {
        List<PushDeviceToken> tokens = tokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No push tokens for user {}", userId);
            return;
        }
        for (PushDeviceToken t : tokens) {
            sendFcmMessage(t.getToken(), title, body);
        }
    }

    private void sendFcmMessage(String token, String title, String body) {
        if (fcmServerKey == null || fcmServerKey.isBlank()) {
            log.warn("FCM server key not configured; skipping push send");
            return;
        }
        String url = "https://fcm.googleapis.com/fcm/send";
        Map<String, Object> payload = Map.of(
                "to", token,
                "notification", Map.of("title", title, "body", body),
                "data", Map.of("type", "generic"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "key=" + fcmServerKey); // Legacy FCM auth
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception ex) {
            log.error("Failed to send FCM message: {}", ex.getMessage());
        }
    }
}
