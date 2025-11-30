package com.healthlink.domain.notification.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateNotificationPreferenceRequest {
    private Boolean appointmentReminderEnabled;
    private Boolean paymentStatusEnabled;
    private Boolean cancellationEnabled;
    private List<Integer> reminderOffsets; // minutes list
}