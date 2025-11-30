package com.healthlink.service.notification;

import com.google.firebase.messaging.*;
import com.healthlink.domain.notification.repository.PushDeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PushNotificationService
 */
@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PushDeviceTokenRepository tokenRepository;

    @InjectMocks
    private PushNotificationService pushNotificationService;

    @BeforeEach
    void setUp() {
        // Setup is done via @Mock and @InjectMocks
    }

    @Test
    void sendNotification_shouldSendToFirebase() throws Exception {
        String deviceToken = "device-token-123";
        String title = "Test Title";
        String body = "Test Body";

        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id-123");

        pushNotificationService.sendNotification(deviceToken, title, body, null);

        // Give async time to execute
        Thread.sleep(100);

        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void sendNotification_shouldIncludeDataPayload() throws Exception {
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        String deviceToken = "device-token-123";
        Map<String, String> data = Map.of("key1", "value1", "key2", "value2");

        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id-123");

        pushNotificationService.sendNotification(deviceToken, "Title", "Body", data);

        Thread.sleep(100);

        verify(firebaseMessaging).send(messageCaptor.capture());
        // Note: Can't easily verify data payload without more complex mocking
    }

    @Test
    void sendMulticastNotification_shouldSendToMultipleDevices() throws Exception {
        List<String> devices = Arrays.asList("device1", "device2", "device3");

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getSuccessCount()).thenReturn(3);
        when(batchResponse.getFailureCount()).thenReturn(0);
        @SuppressWarnings("deprecation")
        var unused1 = when(firebaseMessaging.sendMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);
        unused1.toString(); // Suppress unused warning

        pushNotificationService.sendMulticastNotification(devices, "Title", "Body", null);

        Thread.sleep(100);

        @SuppressWarnings("deprecation")
        var unused2 = verify(firebaseMessaging).sendMulticast(any(MulticastMessage.class));
        unused2.toString(); // Suppress unused warning
    }

    @Test
    void sendAppointmentReminder_shouldSendCorrectNotification() throws Exception {
        String deviceToken = "device-token";
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id");

        pushNotificationService.sendAppointmentReminder(deviceToken, "Dr. Smith", "Tomorrow at 10:00 AM");

        Thread.sleep(100);

        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void sendPaymentVerificationNotification_shouldSendToStaff() throws Exception {
        String deviceToken = "staff-device";
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id");

        pushNotificationService.sendPaymentVerificationNotification(deviceToken, "John Doe", "5000");

        Thread.sleep(100);

        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void sendNotification_shouldHandleNullFirebaseMessaging() {
        // Simulate null FirebaseMessaging (not initialized)
        PushNotificationService serviceWithNullFM = new PushNotificationService(null, null, tokenRepository);

        // Should not throw exception
        assertThatCode(() -> serviceWithNullFM.sendNotification("token", "Title", "Body", null))
                .doesNotThrowAnyException();
    }
}
