package com.healthlink.domain.notification.dto;

import com.healthlink.domain.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Message format for notification delivery queue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeliveryMessage {
    private UUID notificationId;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String body;
    private Map<String, String> metadata;
    private OffsetDateTime scheduledAt;
    private Integer attemptNumber;
}
