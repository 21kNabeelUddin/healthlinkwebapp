package com.healthlink.domain.notification;

import com.healthlink.domain.notification.entity.Notification;
import com.healthlink.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for basic notification listing and acknowledgement.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> listForUser(UUID userId) {
        return notificationRepository.findAll().stream()
                .filter(n -> userId.equals(n.getUserId()))
                .toList();
    }

    public Notification acknowledge(UUID notificationId, UUID userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!userId.equals(n.getUserId())) {
            throw new IllegalStateException("Cannot acknowledge another user's notification");
        }
        if (n.getDeliveredAt() == null) {
            n.setDeliveredAt(OffsetDateTime.now());
        }
        return notificationRepository.save(n);
    }
}
