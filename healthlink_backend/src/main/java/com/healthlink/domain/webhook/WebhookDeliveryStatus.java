package com.healthlink.domain.webhook;

/**
 * Status of webhook event delivery to external subscribers
 */
public enum WebhookDeliveryStatus {
    /**
     * Event is queued but not yet delivered
     */
    PENDING,
    
    /**
     * Event successfully delivered to subscriber
     */
    DELIVERED,
    
    /**
     * Event delivery failed (4xx/5xx response or network error)
     */
    FAILED,
    
    /**
     * Event delivery is being retried after initial failure
     */
    RETRYING
}
