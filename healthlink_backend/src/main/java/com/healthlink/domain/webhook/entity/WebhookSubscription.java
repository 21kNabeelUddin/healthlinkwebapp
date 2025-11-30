package com.healthlink.domain.webhook.entity;

import com.healthlink.common.entity.BaseEntity;
import com.healthlink.domain.webhook.EventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "webhook_subscriptions")
@Getter
@Setter
public class WebhookSubscription extends BaseEntity {

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    @Column(name = "secret", nullable = false)
    private String secret; // HMAC secret for signature verification

    @Column(name = "active", nullable = false)
    private boolean active = true;
}