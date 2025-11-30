package com.healthlink.domain.notification;

import com.healthlink.infrastructure.logging.SafeLogger;
import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.notification.entity.Notification;
import com.healthlink.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {
    private final SafeLogger log = SafeLogger.get(NotificationScheduler.class);
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    /**
     * Scheduled task for appointment reminders.
     * Runs every 5 minutes to check for upcoming appointments and create notifications
     * based on configured reminder times (1h, 15m, 5m before appointment).
     * 
     * Note: In-app notifications only (NO email, NO SMS per spec).
     */
    @Scheduled(cron = "0 */5 * * * *") // every 5 minutes
    public void processPendingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);
        
        // Fetch confirmed appointments in next hour
        var upcomingAppointments = appointmentRepository
            .findByAppointmentTimeBetweenAndStatus(now, oneHourLater, AppointmentStatus.CONFIRMED);
        
        int notificationsCreated = 0;
        for (Appointment appt : upcomingAppointments) {
            notificationsCreated += createRemindersIfNeeded(appt, now);
        }
        
        log.event("notification_scheduler_completed")
            .with("appointments_checked", upcomingAppointments.size())
            .with("notifications_created", notificationsCreated)
            .log();
    }

    /**
     * Creates reminder notifications for an appointment if within reminder windows.
     * Reminder windows (with tolerance):
     * - 1 hour: 55-65 minutes before
     * - 15 minutes: 13-17 minutes before
     * - 5 minutes: 4-6 minutes before
     * 
     * @return count of notifications created (0-3)
     */
    private int createRemindersIfNeeded(Appointment appt, LocalDateTime now) {
        long minutesUntil = ChronoUnit.MINUTES.between(now, appt.getAppointmentTime());
        int created = 0;
        
        // 1-hour reminder (55-65 minute window)
        if (minutesUntil >= 55 && minutesUntil <= 65) {
            if (createNotification(appt, "in 1 hour")) {
                created++;
            }
        }
        
        // 15-minute reminder (13-17 minute window)
        if (minutesUntil >= 13 && minutesUntil <= 17) {
            if (createNotification(appt, "in 15 minutes")) {
                created++;
            }
        }
        
        // 5-minute reminder (4-6 minute window)
        if (minutesUntil >= 4 && minutesUntil <= 6) {
            if (createNotification(appt, "in 5 minutes")) {
                created++;
            }
        }
        
        return created;
    }

    /**
     * Creates a notification entity for an appointment reminder.
     * Uses message uniqueness to prevent duplicates (idempotent).
     * 
     * @return true if notification was created, false if duplicate
     */
    private boolean createNotification(Appointment appt, String timing) {
        String doctorName = String.format("Dr. %s %s", 
            appt.getDoctor().getFirstName(), 
            appt.getDoctor().getLastName());
        
        String message = String.format(
            "Reminder: Appointment with %s %s at %s",
            doctorName,
            timing,
            appt.getAppointmentTime().format(TIME_FORMATTER)
        );
        
        // Check for duplicate notification (idempotent)
        if (notificationRepository.existsByUserIdAndTypeAndMessage(
                appt.getPatient().getId(),
                NotificationType.APPOINTMENT_REMINDER,
                message)) {
            return false;
        }
        
        Notification notification = new Notification();
        notification.setUserId(appt.getPatient().getId());
        notification.setType(NotificationType.APPOINTMENT_REMINDER);
        notification.setMessage(message);
        notification.setScheduledAt(OffsetDateTime.now(ZoneOffset.UTC));
        
        notificationRepository.save(notification);
        
        log.event("notification_created")
            .with("type", NotificationType.APPOINTMENT_REMINDER.name())
            .with("timing", timing)
            .log();
        
        return true;
    }
}