package com.healthlink.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Webhook Service
 * Emits events to registered webhook endpoints for external integrations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // TODO: Load from database/config - webhook subscriptions per organization
    private final Map<String, WebhookSubscription> webhookSubscriptions = new HashMap<>();

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Emit webhook event asynchronously
     */
    @Async
    public void emitEvent(WebhookEvent event) {
        log.debug("Emitting webhook event: {} for entity: {}", event.getEventType(), event.getEntityId());

        // Get all subscriptions for this event type
        webhookSubscriptions.values().stream()
                .filter(sub -> sub.getSubscribedEvents().contains(event.getEventType()))
                .forEach(subscription -> sendWebhook(subscription, event));
    }

    /**
     * Send webhook to specific endpoint
     */
    private void sendWebhook(WebhookSubscription subscription, WebhookEvent event) {
        try {
            // Prepare payload
            WebhookPayload payload = WebhookPayload.builder()
                    .id(UUID.randomUUID().toString())
                    .eventType(event.getEventType())
                    .entityId(event.getEntityId())
                    .entityType(event.getEntityType())
                    .data(event.getData())
                    .timestamp(Instant.now().toString())
                    .build();

            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Generate HMAC signature for security
            String signature = generateHmacSignature(jsonPayload, subscription.getSecret());

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Signature", signature);
            headers.set("X-Webhook-Event-Type", event.getEventType());
            headers.set("X-Webhook-Delivery-Id", payload.getId());

            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            // Send webhook (with timeout and retry logic in production)
            restTemplate.postForEntity(subscription.getUrl(), request, String.class);

            log.info("Webhook sent successfully: {} to {}", event.getEventType(), subscription.getUrl());

        } catch (Exception e) {
            log.error("Failed to send webhook to {}: {}", subscription.getUrl(), e.getMessage());
            // TODO: Implement retry queue for failed webhooks
        }
    }

    /**
     * Generate HMAC-SHA256 signature for webhook security
     */
    private String generateHmacSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }

    /**
     * Register webhook subscription (for testing/admin)
     */
    public void registerSubscription(WebhookSubscription subscription) {
        webhookSubscriptions.put(subscription.getId(), subscription);
        log.info("Registered webhook subscription: {} for events: {}",
                subscription.getUrl(), subscription.getSubscribedEvents());
    }

    /**
     * Webhook Event
     */
    @lombok.Data
    @lombok.Builder
    public static class WebhookEvent {
        private String eventType; // e.g., "appointment.created", "payment.verified"
        private String entityId;
        private String entityType;
        private Map<String, Object> data;
    }

    /**
     * Webhook Payload sent to endpoints
     */
    @lombok.Data
    @lombok.Builder
    private static class WebhookPayload {
        private String id;
        private String eventType;
        private String entityId;
        private String entityType;
        private Map<String, Object> data;
        private String timestamp;
    }

    /**
     * Webhook Subscription configuration
     */
    @lombok.Data
    @lombok.Builder
    public static class WebhookSubscription {
        private String id;
        private String url;
        private String secret; // For HMAC signature verification
        private java.util.Set<String> subscribedEvents;
    }

    // Predefined event types
    public static class EventTypes {
        public static final String APPOINTMENT_CREATED = "appointment.created";
        public static final String APPOINTMENT_UPDATED = "appointment.updated";
        public static final String APPOINTMENT_CANCELLED = "appointment.cancelled";
        public static final String PAYMENT_INITIATED = "payment.initiated";
        public static final String PAYMENT_VERIFIED = "payment.verified";
        public static final String PAYMENT_FAILED = "payment.failed";
        public static final String PRESCRIPTION_CREATED = "prescription.created";
        public static final String LAB_RESULT_UPLOADED = "lab_result.uploaded";
        public static final String USER_APPROVED = "user.approved";
        public static final String USER_REJECTED = "user.rejected";
    }
}
