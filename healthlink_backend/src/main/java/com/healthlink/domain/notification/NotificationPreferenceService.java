package com.healthlink.domain.notification;

import com.healthlink.domain.notification.dto.NotificationPreferenceResponse;
import com.healthlink.domain.notification.dto.UpdateNotificationPreferenceRequest;
import com.healthlink.domain.notification.entity.NotificationPreference;
import com.healthlink.domain.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse get(UUID userId) {
        NotificationPreference pref = repository.findByUserId(userId)
                .orElseGet(() -> createDefault(userId));
        return toResponse(pref);
    }

    @Transactional
    public NotificationPreferenceResponse update(UUID userId, UpdateNotificationPreferenceRequest request) {
        NotificationPreference pref = repository.findByUserId(userId)
                .orElseGet(() -> createDefault(userId));
        if (request.getAppointmentReminderEnabled() != null) pref.setAppointmentReminderEnabled(request.getAppointmentReminderEnabled());
        if (request.getPaymentStatusEnabled() != null) pref.setPaymentStatusEnabled(request.getPaymentStatusEnabled());
        if (request.getCancellationEnabled() != null) pref.setCancellationEnabled(request.getCancellationEnabled());
        if (request.getReminderOffsets() != null && !request.getReminderOffsets().isEmpty()) {
            pref.setReminderOffsets(request.getReminderOffsets().stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        repository.save(pref);
        return toResponse(pref);
    }

    private NotificationPreference createDefault(UUID userId) {
        NotificationPreference pref = new NotificationPreference();
        pref.setUserId(userId);
        return repository.save(pref);
    }

    private NotificationPreferenceResponse toResponse(NotificationPreference pref) {
        List<Integer> offsets = Arrays.stream(pref.getReminderOffsets().split(","))
                .filter(s -> !s.isBlank())
                .map(Integer::valueOf)
                .collect(Collectors.toList());
        return NotificationPreferenceResponse.builder()
                .userId(pref.getUserId())
                .appointmentReminderEnabled(pref.isAppointmentReminderEnabled())
                .paymentStatusEnabled(pref.isPaymentStatusEnabled())
                .cancellationEnabled(pref.isCancellationEnabled())
                .reminderOffsets(offsets)
                .build();
    }
}