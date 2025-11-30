package com.healthlink.domain.webhook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Message format for webhook delivery queue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDeliveryMessage {
    private UUID eventId;
    private UUID subscriptionId;
    private String targetUrl;
    private String eventType;
    private Map<String, Object> payload;
    private String signature; // HMAC SHA256
    private Integer attemptNumber;
    private OffsetDateTime scheduledAt;
}
