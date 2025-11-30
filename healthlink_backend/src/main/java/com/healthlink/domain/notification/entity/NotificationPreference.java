package com.healthlink.domain.notification.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
public class NotificationPreference extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    // Legacy preference fields retained for backward compatibility
    @Column(name = "appointment_reminder_enabled", nullable = false)
    private boolean appointmentReminderEnabled = true;

    @Column(name = "payment_status_enabled", nullable = false)
    private boolean paymentStatusEnabled = true;

    @Column(name = "cancellation_enabled", nullable = false)
    private boolean cancellationEnabled = true;

    @Column(name = "reminder_offsets", length = 100)
    private String reminderOffsets = "60,15,5"; // minutes before appointment

    // New granular preference controls
    @Column(name = "appointment_reminders", nullable = false)
    private boolean appointmentReminders = true;

    @Column(name = "appointment_confirmations", nullable = false)
    private boolean appointmentConfirmations = true;

    @Column(name = "appointment_cancellations", nullable = false)
    private boolean appointmentCancellations = true;

    @Column(name = "payment_updates", nullable = false)
    private boolean paymentUpdates = true;

    @Column(name = "video_call_notifications", nullable = false)
    private boolean videoCallNotifications = true;

    @Column(name = "prescription_notifications", nullable = false)
    private boolean prescriptionNotifications = true;

    @Column(name = "system_notifications", nullable = false)
    private boolean systemNotifications = true;
}