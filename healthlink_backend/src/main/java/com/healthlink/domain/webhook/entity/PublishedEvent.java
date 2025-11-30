package com.healthlink.domain.webhook.entity;

import com.healthlink.common.entity.BaseEntity;
import com.healthlink.domain.webhook.EventType;
import com.healthlink.domain.webhook.WebhookDeliveryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "published_events")
@Getter
@Setter
public class PublishedEvent extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(name = "published_at", nullable = false)
    private OffsetDateTime publishedAt = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private WebhookDeliveryStatus deliveryStatus = WebhookDeliveryStatus.PENDING;
    
    @Column(name = "delivery_attempts")
    private Integer deliveryAttempts = 0;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;
}