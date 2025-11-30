package com.healthlink.service.notification;

import com.google.firebase.messaging.*;
import com.healthlink.domain.notification.entity.PushDeviceToken;
import com.healthlink.domain.notification.repository.PushDeviceTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Push Notification Service using Firebase Cloud Messaging.
 * This service consolidates push notification functionality using Firebase Admin SDK.
 * 
 * @see com.healthlink.domain.notification.service.LegacyPushNotificationService (deprecated)
 */
@Service
@Slf4j
public class PushNotificationService {

    private final FirebaseMessaging firebaseMessaging;
    @SuppressWarnings("unused")
    private final RabbitTemplate rabbitTemplate; // Reserved for future async notification queue
    private final PushDeviceTokenRepository tokenRepository;

    public PushNotificationService(
            @Nullable FirebaseMessaging firebaseMessaging,
            RabbitTemplate rabbitTemplate,
            PushDeviceTokenRepository tokenRepository) {
        this.firebaseMessaging = firebaseMessaging;
        this.rabbitTemplate = rabbitTemplate;
        this.tokenRepository = tokenRepository;
    }
    
    /**
     * Register or update device token for a user.
     * Supports multiple devices per user.
     * 
     * @param userId User ID
     * @param token FCM device token
     * @param platform Platform (ANDROID, IOS, WEB)
     */
    public void registerToken(UUID userId, String token, String platform) {
        var existing = tokenRepository.findByToken(token).orElse(null);
        if (existing != null) {
            existing.setUserId(userId);
            existing.setPlatform(platform);
            existing.setLastSeenAt(Instant.now());
            tokenRepository.save(existing);
            log.info("Updated push token for user {}", userId);
            return;
        }
        
        PushDeviceToken dt = new PushDeviceToken();
        dt.setUserId(userId);
        dt.setToken(token);
        dt.setPlatform(platform);
        dt.setLastSeenAt(Instant.now());
        tokenRepository.save(dt);
        log.info("Registered new push token for user {}", userId);
    }
    
    /**
     * Send notification to all devices of a user.
     * 
     * @param userId Target user ID
     * @param title Notification title
     * @param body Notification body
     */
    @Async
    public void sendToUser(UUID userId, String title, String body) {
        sendToUser(userId, title, body, null);
    }
    
    /**
     * Send notification to all devices of a user with custom data.
     * 
     * @param userId Target user ID
     * @param title Notification title
     * @param body Notification body
     * @param data Custom data payload
     */
    @Async
    public void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        List<PushDeviceToken> tokens = tokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No push tokens found for user {}", userId);
            return;
        }
        
        List<String> deviceTokens = tokens.stream()
                .map(PushDeviceToken::getToken)
                .toList();
        
        if (deviceTokens.size() == 1) {
            sendNotification(deviceTokens.get(0), title, body, data);
        } else {
            sendMulticastNotification(deviceTokens, title, body, data);
        }
    }

    /**
     * Send push notification to a specific device
     */
    @Async
    public void sendNotification(String deviceToken, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.warn("Firebase Messaging not initialized. Skipping push notification.");
            return;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String response = firebaseMessaging.send(messageBuilder.build());
            log.info("Push notification sent successfully: {}", response);

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send push notification", e);
            // TODO: Handle invalid tokens, update database
        }
    }

    /**
     * Send notification to multiple devices
     */
    @Async
    public void sendMulticastNotification(java.util.List<String> deviceTokens,
            String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.warn("Firebase Messaging not initialized. Skipping multicast notification.");
            return;
        }

        try {
            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .addAllTokens(deviceTokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            @SuppressWarnings("deprecation")
            BatchResponse response = firebaseMessaging.sendMulticast(messageBuilder.build());
            log.info("Multicast notification sent. Success: {}, Failure: {}",
                    response.getSuccessCount(), response.getFailureCount());

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send multicast notification", e);
        }
    }

    /**
     * Send appointment reminder notification
     */
    public void sendAppointmentReminder(String deviceToken, String doctorName, String appointmentTime) {
        Map<String, String> data = Map.of(
                "type", "appointment_reminder",
                "doctorName", doctorName,
                "appointmentTime", appointmentTime);

        sendNotification(
                deviceToken,
                "Appointment Reminder",
                String.format("You have an appointment with Dr. %s at %s", doctorName, appointmentTime),
                data);
    }

    /**
     * Send payment verification notification to staff
     */
    public void sendPaymentVerificationNotification(String deviceToken, String patientName, String amount) {
        Map<String, String> data = Map.of(
                "type", "payment_verification",
                "patientName", patientName,
                "amount", amount);

        sendNotification(
                deviceToken,
                "Payment Verification Required",
                String.format("New payment receipt from %s for PKR %s", patientName, amount),
                data);
    }

    /**
     * Send approval notification
     */
    public void sendApprovalNotification(String deviceToken, boolean approved, String roleName) {
        String title = approved ? "Account Approved" : "Account Rejected";
        String body = approved
                ? String.format("Your %s account has been approved. You can now log in.", roleName)
                : String.format("Your %s account application has been rejected. Please contact support.", roleName);

        Map<String, String> data = Map.of(
                "type", "approval",
                "approved", String.valueOf(approved),
                "role", roleName);

        sendNotification(deviceToken, title, body, data);
    }
}
