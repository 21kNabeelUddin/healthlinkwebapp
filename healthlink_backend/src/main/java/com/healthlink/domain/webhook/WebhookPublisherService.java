package com.healthlink.domain.webhook;

import com.healthlink.domain.webhook.dto.WebhookDeliveryMessage;
import com.healthlink.domain.webhook.entity.PublishedEvent;
import com.healthlink.domain.webhook.repository.PublishedEventRepository;
import com.healthlink.domain.webhook.repository.WebhookSubscriptionRepository;
import com.healthlink.infrastructure.logging.SafeLogger;
import com.healthlink.infrastructure.messaging.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Webhook publisher service - publishes events to RabbitMQ for async delivery.
 */
@Service
@RequiredArgsConstructor
public class WebhookPublisherService {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final PublishedEventRepository eventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SafeLogger log = SafeLogger.get(WebhookPublisherService.class);

    @Transactional
    public void publish(EventType type, String referenceId) {
        publish(type, referenceId, Map.of());
    }

    @Transactional
    public void publish(EventType type, String referenceId, Map<String, Object> additionalData) {
        var subscriptions = subscriptionRepository.findByEventTypeAndActiveTrue(type);
        
        if (subscriptions.isEmpty()) {
            log.event("webhook_no_subscribers")
               .with("eventType", type.name())
               .with("referenceId", referenceId)
               .log();
            return;
        }

        log.event("webhook_publish")
           .with("eventType", type.name())
           .with("referenceId", referenceId)
           .with("subscriberCount", String.valueOf(subscriptions.size()))
           .log();

        subscriptions.forEach(subscription -> {
            // Create event record
            PublishedEvent event = new PublishedEvent();
            event.setEventType(type);
            event.setReferenceId(referenceId);
            event.setTargetUrl(subscription.getTargetUrl());
            event.setDeliveryStatus(WebhookDeliveryStatus.PENDING);
            event.setDeliveryAttempts(0);
            PublishedEvent savedEvent = eventRepository.save(event);

            // Prepare payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", type.name());
            payload.put("eventId", savedEvent.getId().toString());
            payload.put("referenceId", referenceId);
            payload.put("timestamp", OffsetDateTime.now().toString());
            payload.putAll(additionalData);

            // Enqueue for async delivery
            WebhookDeliveryMessage message = WebhookDeliveryMessage.builder()
                    .eventId(savedEvent.getId())
                    .subscriptionId(subscription.getId())
                    .targetUrl(subscription.getTargetUrl())
                    .eventType(type.name())
                    .payload(payload)
                    .attemptNumber(1)
                    .scheduledAt(OffsetDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.WEBHOOK_EXCHANGE,
                    RabbitMQConfig.WEBHOOK_ROUTING_KEY,
                    message
            );

            log.event("webhook_enqueued")
               .with("eventId", savedEvent.getId().toString())
               .with("subscriptionId", subscription.getId().toString())
               .log();
        });
    }
}