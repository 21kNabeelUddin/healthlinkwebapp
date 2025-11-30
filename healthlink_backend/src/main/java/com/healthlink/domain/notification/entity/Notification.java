package com.healthlink.domain.notification.entity;

import com.healthlink.common.entity.BaseEntity;
import com.healthlink.domain.notification.NotificationType;
import com.healthlink.domain.notification.NotificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "metadata", length = 4000)
    private String metadata;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;
}