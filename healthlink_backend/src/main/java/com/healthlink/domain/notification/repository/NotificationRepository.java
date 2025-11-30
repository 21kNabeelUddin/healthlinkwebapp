package com.healthlink.domain.notification.repository;

import com.healthlink.domain.notification.NotificationType;
import com.healthlink.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdAndDeliveredAtIsNullAndScheduledAtBefore(UUID userId, OffsetDateTime before);
    
    boolean existsByUserIdAndTypeAndMessage(UUID userId, NotificationType type, String message);
}