package com.healthlink.domain.admin.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlink.domain.admin.notification.dto.*;
import com.healthlink.domain.admin.notification.entity.*;
import com.healthlink.domain.admin.notification.repository.AdminNotificationRepository;
import com.healthlink.domain.notification.NotificationType;
import com.healthlink.domain.notification.NotificationStatus;
import com.healthlink.domain.notification.entity.Notification;
import com.healthlink.domain.notification.repository.NotificationRepository;
import com.healthlink.domain.notification.service.NotificationSchedulerService;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.infrastructure.logging.SafeLogger;
import com.healthlink.service.notification.EmailService;
import com.healthlink.service.notification.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final AdminNotificationRepository adminNotificationRepository;
    private final UserRepository userRepository;
    private final NotificationSchedulerService notificationSchedulerService;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;
    private final ObjectMapper objectMapper;
    private final SafeLogger log = SafeLogger.get(AdminNotificationService.class);

    /**
     * Send custom notification to selected recipients.
     */
    @Transactional
    public AdminNotificationResponse sendNotification(SendCustomNotificationRequest request, UUID adminId) {
        // Determine recipients
        List<UUID> recipientIds = determineRecipients(request);
        
        if (recipientIds.isEmpty()) {
            throw new IllegalArgumentException("No recipients found for the specified criteria");
        }

        // Create admin notification record
        AdminNotification adminNotification = new AdminNotification();
        adminNotification.setSentByAdminId(adminId);
        adminNotification.setTitle(request.getTitle());
        adminNotification.setMessage(request.getMessage());
        adminNotification.setNotificationType(request.getNotificationType());
        adminNotification.setPriority(request.getPriority());
        adminNotification.setRecipientType(request.getRecipientType().name());
        adminNotification.setTotalRecipients(recipientIds.size());
        adminNotification.setChannels(request.getChannels().stream()
                .map(Enum::name)
                .collect(Collectors.joining(",")));
        
        try {
            adminNotification.setRecipientIds(objectMapper.writeValueAsString(recipientIds));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize recipient IDs", e);
        }

        // Handle scheduling
        if (request.getScheduledAt() != null && request.getScheduledAt().isAfter(OffsetDateTime.now())) {
            adminNotification.setScheduledAt(request.getScheduledAt());
            adminNotification.setStatus(AdminNotificationStatus.SCHEDULED);
        } else {
            adminNotification.setStatus(AdminNotificationStatus.SENDING);
            adminNotification.setSentAt(OffsetDateTime.now());
        }

        adminNotification = adminNotificationRepository.save(adminNotification);

        // Send notifications asynchronously
        if (adminNotification.getStatus() == AdminNotificationStatus.SENDING) {
            sendNotificationsAsync(adminNotification, recipientIds, request);
        }

        return AdminNotificationResponse.fromEntity(adminNotification);
    }

    /**
     * Determine recipient IDs based on request type.
     */
    private List<UUID> determineRecipients(SendCustomNotificationRequest request) {
        return switch (request.getRecipientType()) {
            case INDIVIDUAL_USER, INDIVIDUAL_DOCTOR -> {
                if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
                    throw new IllegalArgumentException("Recipient IDs required for individual notifications");
                }
                yield request.getRecipientIds();
            }
            case ALL_USERS -> userRepository.findByRole(UserRole.PATIENT)
                    .stream()
                    .map(user -> user.getId())
                    .collect(Collectors.toList());
            case ALL_DOCTORS -> userRepository.findByRole(UserRole.DOCTOR)
                    .stream()
                    .map(user -> user.getId())
                    .collect(Collectors.toList());
            case SELECTED_USERS -> {
                if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
                    throw new IllegalArgumentException("Recipient IDs required for selected users");
                }
                yield request.getRecipientIds();
            }
            case SELECTED_DOCTORS -> {
                if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
                    throw new IllegalArgumentException("Recipient IDs required for selected doctors");
                }
                yield request.getRecipientIds();
            }
        };
    }

    /**
     * Send notifications asynchronously to all recipients.
     */
    @Async
    public void sendNotificationsAsync(AdminNotification adminNotification, List<UUID> recipientIds, 
                                      SendCustomNotificationRequest request) {
        int sentCount = 0;
        int failedCount = 0;

        for (UUID recipientId : recipientIds) {
            try {
                // Send via each selected channel
                for (SendCustomNotificationRequest.NotificationChannel channel : request.getChannels()) {
                    switch (channel) {
                        case IN_APP -> {
                            try {
                                notificationSchedulerService.scheduleNotification(
                                        recipientId,
                                        NotificationType.APPOINTMENT_REMINDER, // Using existing type, could add ADMIN_MESSAGE
                                        request.getTitle(),
                                        request.getMessage()
                                );
                            } catch (Exception e) {
                                // Fallback: Create notification directly if RabbitMQ is unavailable
                                log.warn("RabbitMQ unavailable, creating notification directly for user {}", recipientId, e);
                                createNotificationDirectly(recipientId, request.getTitle(), request.getMessage());
                            }
                        }
                        case EMAIL -> {
                            userRepository.findById(recipientId)
                                    .ifPresent(user -> {
                                        try {
                                            emailService.sendSimpleEmail(
                                                    user.getEmail(),
                                                    request.getTitle(),
                                                    request.getMessage()
                                            );
                                        } catch (Exception e) {
                                            log.error("Failed to send email to user {}", recipientId, e);
                                        }
                                    });
                        }
                        case PUSH -> {
                            pushNotificationService.sendToUser(
                                    recipientId,
                                    request.getTitle(),
                                    request.getMessage()
                            );
                        }
                        case SMS -> {
                            // SMS service integration would go here
                            log.info("SMS notification requested for user {}, but SMS service not yet implemented", recipientId);
                        }
                    }
                }
                sentCount++;
            } catch (Exception e) {
                log.error("Failed to send notification to user {}", recipientId, e);
                failedCount++;
            }
        }

        // Update notification status
        adminNotification.setSentCount(sentCount);
        adminNotification.setFailedCount(failedCount);
        adminNotification.setStatus(AdminNotificationStatus.SENT);
        adminNotificationRepository.save(adminNotification);
    }

    /**
     * Get notification history for an admin.
     */
    @Transactional(readOnly = true)
    public NotificationHistoryResponse getNotificationHistory(UUID adminId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminNotification> notificationPage = adminNotificationRepository
                .findBySentByAdminIdOrderByCreatedAtDesc(adminId, pageable);

        List<AdminNotificationResponse> notifications = notificationPage.getContent().stream()
                .map(AdminNotificationResponse::fromEntity)
                .collect(Collectors.toList());

        return NotificationHistoryResponse.builder()
                .notifications(notifications)
                .totalCount(notificationPage.getTotalElements())
                .page(page)
                .pageSize(size)
                .totalPages(notificationPage.getTotalPages())
                .build();
    }

    /**
     * Process scheduled notifications (called by scheduler).
     */
    @Transactional
    public void processScheduledNotifications() {
        List<AdminNotification> scheduled = adminNotificationRepository
                .findByStatusAndScheduledAtBefore(AdminNotificationStatus.SCHEDULED, OffsetDateTime.now());

        for (AdminNotification notification : scheduled) {
            try {
                List<UUID> recipientIds = parseRecipientIds(notification.getRecipientIds());
                SendCustomNotificationRequest request = createRequestFromNotification(notification);
                
                notification.setStatus(AdminNotificationStatus.SENDING);
                notification.setSentAt(OffsetDateTime.now());
                adminNotificationRepository.save(notification);
                
                sendNotificationsAsync(notification, recipientIds, request);
            } catch (Exception e) {
                log.error("Failed to process scheduled notification {}", notification.getId(), e);
                notification.setStatus(AdminNotificationStatus.FAILED);
                adminNotificationRepository.save(notification);
            }
        }
    }

    private List<UUID> parseRecipientIds(String recipientIdsJson) {
        try {
            return Arrays.asList(objectMapper.readValue(recipientIdsJson, UUID[].class));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse recipient IDs", e);
            return Collections.emptyList();
        }
    }

    private SendCustomNotificationRequest createRequestFromNotification(AdminNotification notification) {
        SendCustomNotificationRequest request = new SendCustomNotificationRequest();
        request.setTitle(notification.getTitle());
        request.setMessage(notification.getMessage());
        request.setNotificationType(notification.getNotificationType());
        request.setPriority(notification.getPriority());
        request.setChannels(Arrays.stream(notification.getChannels().split(","))
                .map(SendCustomNotificationRequest.NotificationChannel::valueOf)
                .collect(Collectors.toList()));
        return request;
    }

    /**
     * Create notification directly in database when RabbitMQ is unavailable.
     */
    private void createNotificationDirectly(UUID userId, String title, String message) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(NotificationType.APPOINTMENT_REMINDER);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setStatus(NotificationStatus.UNREAD);
            notification.setScheduledAt(OffsetDateTime.now());
            notificationRepository.save(notification);
            log.info("Created notification directly for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to create notification directly for user {}", userId, e);
            throw e;
        }
    }
}

