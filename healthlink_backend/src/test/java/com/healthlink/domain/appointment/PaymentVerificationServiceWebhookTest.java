package com.healthlink.domain.appointment;

import com.healthlink.domain.appointment.dto.PaymentVerificationResponse;
import com.healthlink.domain.appointment.entity.Payment;
import com.healthlink.domain.appointment.entity.PaymentVerification;
import com.healthlink.domain.appointment.entity.PaymentVerificationStatus;
import com.healthlink.domain.appointment.repository.PaymentRepository;
import com.healthlink.domain.appointment.repository.PaymentVerificationRepository;
import com.healthlink.domain.appointment.service.PaymentVerificationService;
import com.healthlink.domain.webhook.EventType;
import com.healthlink.domain.webhook.WebhookPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentVerificationService webhook integration
 * 
 * Tests verify that webhook events are published correctly:
 * - PAYMENT_VERIFIED event published ONLY when status = VERIFIED
 * - No webhook for REJECTED or PENDING statuses
 */
@ExtendWith(MockitoExtension.class)
class PaymentVerificationServiceWebhookTest {

    @Mock
    private PaymentVerificationRepository paymentVerificationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WebhookPublisherService webhookPublisher;

    @InjectMocks
    private PaymentVerificationService paymentVerificationService;

    private UUID verificationId;
    private UUID paymentId;
    private PaymentVerification paymentVerification;
    private Payment payment;

    @BeforeEach
    void setUp() {
        verificationId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        
        payment = new Payment();
        payment.setId(paymentId);
        
        paymentVerification = new PaymentVerification();
        paymentVerification.setId(verificationId);
        paymentVerification.setPayment(payment);
        paymentVerification.setVerifierUserId(UUID.randomUUID());
        paymentVerification.setStatus(PaymentVerificationStatus.PENDING_QUEUE);
    }

    @Test
    void verify_withVerifiedStatus_shouldPublishPaymentVerifiedEvent() {
        // Arrange
        when(paymentVerificationRepository.findById(verificationId)).thenReturn(Optional.of(paymentVerification));
        when(paymentVerificationRepository.save(any(PaymentVerification.class))).thenReturn(paymentVerification);
        doNothing().when(webhookPublisher).publish(any(EventType.class), anyString());

        // Act
        PaymentVerificationResponse result = paymentVerificationService.verify(verificationId, PaymentVerificationStatus.VERIFIED, "Receipt valid");

        // Assert
        assertNotNull(result);
        
        ArgumentCaptor<PaymentVerification> verificationCaptor = ArgumentCaptor.forClass(PaymentVerification.class);
        verify(paymentVerificationRepository, times(1)).save(verificationCaptor.capture());
        
        PaymentVerification savedVerification = verificationCaptor.getValue();
        assertEquals(PaymentVerificationStatus.VERIFIED, savedVerification.getStatus());
        
        ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
        ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(webhookPublisher, times(1)).publish(eventTypeCaptor.capture(), referenceIdCaptor.capture());
        
        assertEquals(EventType.PAYMENT_VERIFIED, eventTypeCaptor.getValue());
        assertEquals(paymentId.toString(), referenceIdCaptor.getValue());
    }

    @Test
    void verify_withRejectedStatus_shouldNotPublishPaymentVerifiedEvent() {
        // Arrange
        when(paymentVerificationRepository.findById(verificationId)).thenReturn(Optional.of(paymentVerification));
        when(paymentVerificationRepository.save(any(PaymentVerification.class))).thenReturn(paymentVerification);

        // Act
        PaymentVerificationResponse result = paymentVerificationService.verify(verificationId, PaymentVerificationStatus.REJECTED, "Invalid receipt");

        // Assert
        assertNotNull(result);
        
        ArgumentCaptor<PaymentVerification> verificationCaptor = ArgumentCaptor.forClass(PaymentVerification.class);
        verify(paymentVerificationRepository, times(1)).save(verificationCaptor.capture());
        
        PaymentVerification savedVerification = verificationCaptor.getValue();
        assertEquals(PaymentVerificationStatus.REJECTED, savedVerification.getStatus());
        
        // Critical: no webhook should be published for REJECTED status
        verify(webhookPublisher, never()).publish(any(EventType.class), anyString());
    }

    @Test
    void verify_withPendingStatus_shouldNotPublishEvent() {
        // Arrange
        when(paymentVerificationRepository.findById(verificationId)).thenReturn(Optional.of(paymentVerification));
        when(paymentVerificationRepository.save(any(PaymentVerification.class))).thenReturn(paymentVerification);

        // Act
        PaymentVerificationResponse result = paymentVerificationService.verify(verificationId, PaymentVerificationStatus.PENDING_QUEUE, "Still pending");

        // Assert
        assertNotNull(result);
        verify(webhookPublisher, never()).publish(any(EventType.class), anyString());
    }

    @Test
    void verify_whenVerificationNotFound_shouldNotPublishEvent() {
        // Arrange
        when(paymentVerificationRepository.findById(verificationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> paymentVerificationService.verify(verificationId, PaymentVerificationStatus.VERIFIED, "test"));
        verify(webhookPublisher, never()).publish(any(EventType.class), anyString());
    }

    @Test
    void verify_whenSaveFails_shouldNotPublishEvent() {
        // Arrange
        when(paymentVerificationRepository.findById(verificationId)).thenReturn(Optional.of(paymentVerification));
        when(paymentVerificationRepository.save(any(PaymentVerification.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> paymentVerificationService.verify(verificationId, PaymentVerificationStatus.VERIFIED, "test"));
        verify(webhookPublisher, never()).publish(any(EventType.class), anyString());
    }

    @Test
    void verify_multipleVerifications_shouldPublishOnlyForVerifiedStatus() {
        // Arrange
        PaymentVerification verification1 = new PaymentVerification();
        verification1.setId(UUID.randomUUID());
        verification1.setPayment(payment);
        verification1.setStatus(PaymentVerificationStatus.PENDING_QUEUE);

        PaymentVerification verification2 = new PaymentVerification();
        verification2.setId(UUID.randomUUID());
        verification2.setPayment(payment);
        verification2.setStatus(PaymentVerificationStatus.PENDING_QUEUE);

        when(paymentVerificationRepository.findById(verification1.getId())).thenReturn(Optional.of(verification1));
        when(paymentVerificationRepository.findById(verification2.getId())).thenReturn(Optional.of(verification2));
        when(paymentVerificationRepository.save(any(PaymentVerification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(webhookPublisher).publish(any(EventType.class), anyString());

        // Act
        paymentVerificationService.verify(verification1.getId(), PaymentVerificationStatus.VERIFIED, "Approved");
        paymentVerificationService.verify(verification2.getId(), PaymentVerificationStatus.REJECTED, "Rejected");

        // Assert
        // Only 1 webhook call should happen (for VERIFIED status)
        verify(webhookPublisher, times(1)).publish(eq(EventType.PAYMENT_VERIFIED), eq(paymentId.toString()));
    }
}
