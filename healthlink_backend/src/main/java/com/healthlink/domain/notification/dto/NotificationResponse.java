package com.healthlink.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notification response DTO matching mobile client expectations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private String type; // APPOINTMENT_REMINDER, PAYMENT_ALERT, etc.
    private String title;
    private String message;
    private List<String> channels; // EMAIL, PUSH, SMS
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private Map<String, Object> metadata; // appointmentId, paymentId, etc.
}
