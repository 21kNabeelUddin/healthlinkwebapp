package com.healthlink.domain.webhook.dto;

import com.healthlink.domain.webhook.EventType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for WebhookSubscription responses.
 * Exposes only necessary fields without internal implementation details.
 */
@Data
@Builder
public class WebhookSubscriptionResponse {
    private UUID id;
    private EventType eventType;
    private String targetUrl;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
