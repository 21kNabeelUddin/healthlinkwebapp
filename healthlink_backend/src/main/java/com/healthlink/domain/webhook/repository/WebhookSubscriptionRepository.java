package com.healthlink.domain.webhook.repository;

import com.healthlink.domain.webhook.entity.WebhookSubscription;
import com.healthlink.domain.webhook.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {
    List<WebhookSubscription> findByEventTypeAndActiveTrue(EventType eventType);
    List<WebhookSubscription> findByOwnerUserId(UUID ownerUserId);
}