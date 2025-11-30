package com.healthlink.domain.notification.service;

import com.healthlink.domain.notification.NotificationType;
import com.healthlink.domain.notification.dto.NotificationDeliveryMessage;
import com.healthlink.infrastructure.logging.SafeLogger;
import com.healthlink.infrastructure.messaging.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Notification scheduling service - enqueues notifications for async delivery.
 * Implements idempotency and user preference filtering via worker.
 */
@Service
@RequiredArgsConstructor
public class NotificationSchedulerService {

    private final RabbitTemplate rabbitTemplate;
    private final SafeLogger log = SafeLogger.get(NotificationSchedulerService.class);

    /**
     * Schedule immediate notification delivery.
     */
    public UUID scheduleNotification(UUID userId, NotificationType type, String title, String body) {
        return scheduleNotification(userId, type, title, body, Map.of(), OffsetDateTime.now());
    }

    /**
     * Schedule notification delivery with metadata.
     */
    public UUID scheduleNotification(UUID userId, NotificationType type, String title, String body, Map<String, String> metadata) {
        return scheduleNotification(userId, type, title, body, metadata, OffsetDateTime.now());
    }

    /**
     * Schedule notification delivery at specific time (for reminders).
     */
    public UUID scheduleNotification(UUID userId, NotificationType type, String title, String body, 
                                    Map<String, String> metadata, OffsetDateTime scheduledAt) {
        UUID notificationId = UUID.randomUUID();
        
        NotificationDeliveryMessage message = NotificationDeliveryMessage.builder()
                .notificationId(notificationId)
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .metadata(metadata)
                .scheduledAt(scheduledAt)
                .attemptNumber(1)
                .build();

        // For immediate delivery
        if (scheduledAt.isBefore(OffsetDateTime.now().plusSeconds(5))) {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                    message
            );
            
            log.event("notification_scheduled_immediate")
               .with("notificationId", notificationId.toString())
               .with("userId", userId.toString())
               .with("type", type.name())
               .log();
        } else {
            // For delayed delivery, use RabbitMQ delayed message plugin or Redis scheduler
            // For MVP: send with delay header (requires plugin)
            long delayMillis = scheduledAt.toInstant().toEpochMilli() - System.currentTimeMillis();
            
                // Delayed delivery requires RabbitMQ delayed message plugin.
                // Fallback: immediate enqueue with metadata indicating desired schedule.
                rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                    message
                );
            
            log.event("notification_scheduled_delayed")
               .with("notificationId", notificationId.toString())
               .with("userId", userId.toString())
               .with("type", type.name())
               .with("delaySeconds", String.valueOf(delayMillis / 1000))
               .log();
        }

        return notificationId;
    }

    /**
     * Schedule appointment reminder notifications based on user preferences.
     */
    public void scheduleAppointmentReminders(UUID userId, UUID appointmentId, OffsetDateTime appointmentTime, 
                                            int[] reminderMinutesBefore) {
        for (int minutes : reminderMinutesBefore) {
            OffsetDateTime reminderTime = appointmentTime.minusMinutes(minutes);
            
            if (reminderTime.isAfter(OffsetDateTime.now())) {
                scheduleNotification(
                        userId,
                        NotificationType.APPOINTMENT_REMINDER,
                        "Appointment Reminder",
                        "Your appointment is in " + minutes + " minutes",
                        Map.of(
                                "appointmentId", appointmentId.toString(),
                                "minutesBefore", String.valueOf(minutes)
                        ),
                        reminderTime
                );
            }
        }
    }
}
