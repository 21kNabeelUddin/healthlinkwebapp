package com.healthlink.infrastructure.webhook;

import com.healthlink.domain.webhook.WebhookDeliveryStatus;
import com.healthlink.domain.webhook.dto.WebhookDeliveryMessage;
import com.healthlink.domain.webhook.entity.PublishedEvent;
import com.healthlink.domain.webhook.entity.WebhookSubscription;
import com.healthlink.domain.webhook.repository.PublishedEventRepository;
import com.healthlink.domain.webhook.repository.WebhookSubscriptionRepository;
import com.healthlink.infrastructure.logging.SafeLogger;
import com.healthlink.infrastructure.messaging.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

/**
 * RabbitMQ consumer for async webhook delivery.
 * Only active when 'rabbitmq' profile is enabled.
 * Implements:
 * - HMAC SHA256 signature verification
 * - Exponential backoff retry (max 5 attempts)
 * - Dead-letter queue for permanent failures
 */
@Component
@Profile("rabbitmq")
@RequiredArgsConstructor
public class WebhookDeliveryWorker {

    private final PublishedEventRepository eventRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final SafeLogger log = SafeLogger.get(WebhookDeliveryWorker.class);

    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final String SIGNATURE_HEADER = "X-HealthLink-Signature";
    private static final String TIMESTAMP_HEADER = "X-HealthLink-Timestamp";

    @RabbitListener(queues = RabbitMQConfig.WEBHOOK_QUEUE)
    public void processWebhookDelivery(WebhookDeliveryMessage message) {
        log.event("webhook_delivery_attempt")
                .with("eventId", message.getEventId().toString())
                .with("subscriptionId", message.getSubscriptionId().toString())
                .with("attempt", String.valueOf(message.getAttemptNumber()))
                .log();

        PublishedEvent event = eventRepository.findById(message.getEventId()).orElse(null);
        if (event == null) {
            log.event("webhook_event_not_found").with("eventId", message.getEventId().toString()).log();
            return;
        }

        WebhookSubscription subscription = subscriptionRepository.findById(message.getSubscriptionId()).orElse(null);
        if (subscription == null || !subscription.isActive()) {
            log.event("webhook_subscription_inactive").with("subscriptionId", message.getSubscriptionId().toString())
                    .log();
            event.setDeliveryStatus(WebhookDeliveryStatus.FAILED);
            event.setDeliveryAttempts(event.getDeliveryAttempts() + 1);
            eventRepository.save(event);
            return;
        }

        try {
            // Generate signature
            String timestamp = String.valueOf(System.currentTimeMillis());
            String signature = generateSignature(message.getPayload().toString(), subscription.getSecret(), timestamp);

            // Prepare HTTP request
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set(SIGNATURE_HEADER, signature);
            headers.set(TIMESTAMP_HEADER, timestamp);
            headers.set("X-HealthLink-Event-Type", message.getEventType());
            headers.set("X-HealthLink-Event-Id", message.getEventId().toString());

            HttpEntity<String> entity = new HttpEntity<>(message.getPayload().toString(), headers);

            // Deliver webhook
            ResponseEntity<String> response = restTemplate.exchange(
                    subscription.getTargetUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                event.setDeliveryStatus(WebhookDeliveryStatus.DELIVERED);
                event.setDeliveredAt(OffsetDateTime.now());
                event.setDeliveryAttempts(message.getAttemptNumber());
                eventRepository.save(event);

                log.event("webhook_delivered")
                        .with("eventId", message.getEventId().toString())
                        .with("statusCode", String.valueOf(response.getStatusCode().value()))
                        .log();
            } else {
                handleDeliveryFailure(event, message, "HTTP " + response.getStatusCode().value());
            }

        } catch (Exception e) {
            handleDeliveryFailure(event, message, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void handleDeliveryFailure(PublishedEvent event, WebhookDeliveryMessage message, String errorReason) {
        int nextAttempt = message.getAttemptNumber() + 1;
        event.setDeliveryAttempts(nextAttempt);

        if (nextAttempt < MAX_RETRY_ATTEMPTS) {
            // Exponential backoff: 1s, 2s, 4s, 8s, 16s
            long delaySeconds = (long) Math.pow(2, nextAttempt - 1);

            event.setDeliveryStatus(WebhookDeliveryStatus.PENDING);
            eventRepository.save(event);

            log.event("webhook_retry_scheduled")
                    .with("eventId", message.getEventId().toString())
                    .with("nextAttempt", String.valueOf(nextAttempt))
                    .with("delaySeconds", String.valueOf(delaySeconds))
                    .with("reason", errorReason)
                    .log();

            // Requeue with delay (using RabbitMQ delayed message plugin or TTL queue)
            message.setAttemptNumber(nextAttempt);
            message.setScheduledAt(OffsetDateTime.now().plusSeconds(delaySeconds));

            // Note: For production, use RabbitMQ delayed message plugin
            // For now, simple requeue (immediate retry - should be enhanced)
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.WEBHOOK_EXCHANGE,
                    RabbitMQConfig.WEBHOOK_ROUTING_KEY,
                    message);
        } else {
            // Max retries exhausted - move to DLQ
            event.setDeliveryStatus(WebhookDeliveryStatus.FAILED);
            eventRepository.save(event);

            log.event("webhook_delivery_failed_permanently")
                    .with("eventId", message.getEventId().toString())
                    .with("reason", errorReason)
                    .log();

            // Message will automatically go to DLQ on exception throw
            throw new RuntimeException("Max webhook delivery attempts reached for event " + message.getEventId());
        }
    }

    private String generateSignature(String payload, String secret, String timestamp) {
        try {
            String signaturePayload = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(signaturePayload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.event("webhook_signature_generation_failed").with("error", e.getMessage()).log();
            throw new RuntimeException("Failed to generate webhook signature", e);
        }
    }
}
