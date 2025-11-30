package com.healthlink.domain.notification.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class NotificationPreferenceResponse {
    private UUID userId;
    private boolean appointmentReminderEnabled;
    private boolean paymentStatusEnabled;
    private boolean cancellationEnabled;
    private List<Integer> reminderOffsets;
}