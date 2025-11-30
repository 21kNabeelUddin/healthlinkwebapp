package com.healthlink.infrastructure.notification;

import com.healthlink.domain.notification.NotificationStatus;
import com.healthlink.domain.notification.dto.NotificationDeliveryMessage;
import com.healthlink.domain.notification.entity.Notification;
import com.healthlink.domain.notification.entity.NotificationPreference;
import com.healthlink.domain.notification.repository.NotificationPreferenceRepository;
import com.healthlink.domain.notification.repository.NotificationRepository;
import com.healthlink.infrastructure.logging.SafeLogger;
import com.healthlink.infrastructure.messaging.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RabbitMQ consumer for async notification delivery.
 * Only active when 'rabbitmq' profile is enabled.
 * Implements:
 * - User preference filtering
 * - In-app notification storage
 * - Idempotency (no duplicate sends)
 */
@Component
@Profile("rabbitmq")
@RequiredArgsConstructor
public class NotificationDeliveryWorker {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final SafeLogger log = SafeLogger.get(NotificationDeliveryWorker.class);

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void processNotification(NotificationDeliveryMessage message) {
        log.event("notification_delivery_attempt")
                .with("notificationId", message.getNotificationId().toString())
                .with("userId", message.getUserId().toString())
                .with("type", message.getType().name())
                .log();

        // Check if notification already exists (idempotency)
        if (notificationRepository.existsById(message.getNotificationId())) {
            log.event("notification_already_delivered")
                    .with("notificationId", message.getNotificationId().toString())
                    .log();
            return;
        }

        // Check user preferences
        NotificationPreference preference = preferenceRepository
                .findByUserId(message.getUserId())
                .orElse(createDefaultPreference(message.getUserId()));

        if (!shouldDeliver(preference, message)) {
            log.event("notification_filtered_by_preference")
                    .with("notificationId", message.getNotificationId().toString())
                    .with("type", message.getType().name())
                    .log();

            // Still create notification record but mark as read
            createNotification(message, NotificationStatus.READ);
            return;
        }

        // Deliver in-app notification
        try {
            Notification notification = createNotification(message, NotificationStatus.UNREAD);

            log.event("notification_delivered")
                    .with("notificationId", notification.getId().toString())
                    .with("userId", message.getUserId().toString())
                    .log();

        } catch (Exception e) {
            log.event("notification_delivery_failed")
                    .with("notificationId", message.getNotificationId().toString())
                    .with("error", e.getClass().getSimpleName())
                    .log();
            throw new RuntimeException("Failed to deliver notification", e);
        }
    }

    private boolean shouldDeliver(NotificationPreference preference, NotificationDeliveryMessage message) {
        return switch (message.getType()) {
            case APPOINTMENT_REMINDER -> preference.isAppointmentReminders();
            case APPOINTMENT_CONFIRMED -> preference.isAppointmentConfirmations();
            case APPOINTMENT_CANCELED -> preference.isAppointmentCancellations();
            case PAYMENT_VERIFIED, PAYMENT_REJECTED, PAYMENT_DISPUTED -> preference.isPaymentUpdates();
            case VIDEO_CALL_STARTING -> preference.isVideoCallNotifications();
            case PRESCRIPTION_CREATED -> preference.isPrescriptionNotifications();
            default -> true; // System notifications always delivered
        };
    }

    private Notification createNotification(NotificationDeliveryMessage message, NotificationStatus status) {
        Notification notification = new Notification();
        notification.setId(message.getNotificationId());
        notification.setUserId(message.getUserId());
        notification.setType(message.getType());
        notification.setTitle(message.getTitle());
        notification.setMessage(message.getBody());
        notification.setStatus(status);
        notification.setMetadata(message.getMetadata() != null ? message.getMetadata().toString() : null);
        // createdAt handled by JPA auditing (BaseEntity)
        return notificationRepository.save(notification);
    }

    private NotificationPreference createDefaultPreference(UUID userId) {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(userId);
        preference.setAppointmentReminders(true);
        preference.setAppointmentConfirmations(true);
        preference.setAppointmentCancellations(true);
        preference.setPaymentUpdates(true);
        preference.setVideoCallNotifications(true);
        preference.setPrescriptionNotifications(true);
        preference.setSystemNotifications(true);
        return preferenceRepository.save(preference);
    }
}
