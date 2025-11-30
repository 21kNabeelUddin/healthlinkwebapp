package com.healthlink.domain.appointment;

import com.healthlink.domain.appointment.dto.CreatePaymentDisputeRequest;
import com.healthlink.domain.appointment.dto.PaymentDisputeResponse;
import com.healthlink.domain.appointment.entity.PaymentDispute;
import com.healthlink.domain.appointment.entity.PaymentDisputeResolution;
import com.healthlink.domain.appointment.entity.PaymentDisputeStage;
import com.healthlink.domain.appointment.entity.PaymentVerification;
import com.healthlink.domain.appointment.repository.PaymentDisputeHistoryRepository;
import com.healthlink.domain.appointment.repository.PaymentDisputeRepository;
import com.healthlink.domain.appointment.repository.PaymentVerificationRepository;
import com.healthlink.domain.appointment.service.PaymentDisputeService;
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
 * Unit tests for PaymentDisputeService webhook integration
 * 
 * Tests verify that webhook events are published correctly for:
 * - Payment dispute raising (PAYMENT_DISPUTED)
 */
@ExtendWith(MockitoExtension.class)
class PaymentDisputeServiceWebhookTest {

    @Mock
    private PaymentDisputeRepository paymentDisputeRepository;

    @Mock
    private PaymentDisputeHistoryRepository paymentDisputeHistoryRepository;

    @Mock
    private PaymentVerificationRepository paymentVerificationRepository;

    @Mock
    private WebhookPublisherService webhookPublisher;

    @InjectMocks
    private PaymentDisputeService paymentDisputeService;

    private UUID disputeId;
    private UUID verificationId;
    private UUID raisedByUserId;
    private PaymentDispute mockDispute;
    private PaymentVerification mockVerification;
    private CreatePaymentDisputeRequest disputeRequest;

    @BeforeEach
    void setUp() {
        disputeId = UUID.randomUUID();
        verificationId = UUID.randomUUID();
        raisedByUserId = UUID.randomUUID();
        
        mockVerification = new PaymentVerification();
        mockVerification.setId(verificationId);
        mockVerification.setDisputed(false);
        
        mockDispute = new PaymentDispute();
        mockDispute.setId(disputeId);
        mockDispute.setVerification(mockVerification);
        mockDispute.setStage(PaymentDisputeStage.STAFF_REVIEW);
        mockDispute.setResolutionStatus(PaymentDisputeResolution.OPEN);
        mockDispute.setRaisedByUserId(raisedByUserId);
        mockDispute.setNotes("Payment not received");
        
        disputeRequest = new CreatePaymentDisputeRequest();
        disputeRequest.setVerificationId(verificationId);
        disputeRequest.setNotes("Payment not received");
    }

    @Test
    void raise_shouldPublishPaymentDisputedEvent() {
        // Arrange
        when(paymentVerificationRepository.findById(verificationId)).thenReturn(Optional.of(mockVerification));
        when(paymentDisputeRepository.save(any(PaymentDispute.class))).thenReturn(mockDispute);
        doNothing().when(webhookPublisher).publish(any(EventType.class), anyString());

        // Act
        PaymentDisputeResponse result = paymentDisputeService.raise(disputeRequest, raisedByUserId);

        // Assert
        assertNotNull(result);
        assertEquals(disputeId, result.getId());
        
        ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
        ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(webhookPublisher, times(1)).publish(eventTypeCaptor.capture(), referenceIdCaptor.capture());
        
        assertEquals(EventType.PAYMENT_DISPUTED, eventTypeCaptor.getValue());
        assertEquals(disputeId.toString(), referenceIdCaptor.getValue());
    }

    @Test
    void raise_withNullReason_shouldStillPublishEvent() {
        // Arrange
        disputeRequest.setNotes(null);
        mockDispute.setNotes(null);
        
        when(paymentVerificationRepository.findById(verificationId)).thenReturn(Optional.of(mockVerification));
        when(paymentDisputeRepository.save(any(PaymentDispute.class))).thenReturn(mockDispute);
        doNothing().when(webhookPublisher).publish(any(EventType.class), anyString());

        // Act
        PaymentDisputeResponse result = paymentDisputeService.raise(disputeRequest, raisedByUserId);

        // Assert
        assertNotNull(result);
        verify(webhookPublisher, times(1)).publish(eq(EventType.PAYMENT_DISPUTED), eq(disputeId.toString()));
    }

    @Test
    void raise_withEmptyReason_shouldStillPublishEvent() {
        // Arrange
        disputeRequest.setNotes("");
        mockDispute.setNotes("");
        
        when(paymentVerificationRepository.findById(verificationId)).thenReturn(Optional.of(mockVerification));
        when(paymentDisputeRepository.save(any(PaymentDispute.class))).thenReturn(mockDispute);
        doNothing().when(webhookPublisher).publish(any(EventType.class), anyString());

        // Act
        PaymentDisputeResponse result = paymentDisputeService.raise(disputeRequest, raisedByUserId);

        // Assert
        assertNotNull(result);
        verify(webhookPublisher, times(1)).publish(eq(EventType.PAYMENT_DISPUTED), eq(disputeId.toString()));
    }

    @Test
    void raise_whenSaveFails_shouldNotPublishEvent() {
        // Arrange
        when(paymentVerificationRepository.findById(verificationId)).thenReturn(Optional.of(mockVerification));
        when(paymentDisputeRepository.save(any(PaymentDispute.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> paymentDisputeService.raise(disputeRequest, raisedByUserId));
        verify(webhookPublisher, never()).publish(any(EventType.class), anyString());
    }

    @Test
    void raise_multipleDisputes_shouldPublishMultipleEvents() {
        // Arrange
        UUID dispute1Id = UUID.randomUUID();
        UUID dispute2Id = UUID.randomUUID();
        
        PaymentDispute dispute1 = new PaymentDispute();
        dispute1.setId(dispute1Id);
        dispute1.setVerification(mockVerification);
        dispute1.setStage(PaymentDisputeStage.STAFF_REVIEW);
        dispute1.setResolutionStatus(PaymentDisputeResolution.OPEN);

        PaymentDispute dispute2 = new PaymentDispute();
        dispute2.setId(dispute2Id);
        dispute2.setVerification(mockVerification);
        dispute2.setStage(PaymentDisputeStage.STAFF_REVIEW);
        dispute2.setResolutionStatus(PaymentDisputeResolution.OPEN);
        
        CreatePaymentDisputeRequest request1 = new CreatePaymentDisputeRequest();
        request1.setVerificationId(verificationId);
        request1.setNotes("Reason 1");
        
        CreatePaymentDisputeRequest request2 = new CreatePaymentDisputeRequest();
        request2.setVerificationId(verificationId);
        request2.setNotes("Reason 2");

        when(paymentVerificationRepository.findById(verificationId)).thenReturn(Optional.of(mockVerification));
        when(paymentDisputeRepository.save(any(PaymentDispute.class)))
            .thenReturn(dispute1)
            .thenReturn(dispute2);
        doNothing().when(webhookPublisher).publish(any(EventType.class), anyString());

        // Act
        paymentDisputeService.raise(request1, raisedByUserId);
        paymentDisputeService.raise(request2, raisedByUserId);

        // Assert
        verify(webhookPublisher, times(2)).publish(eq(EventType.PAYMENT_DISPUTED), anyString());
    }

    @Test
    void raise_shouldSetCorrectInitialStatus() {
        // Arrange
        when(paymentVerificationRepository.findById(verificationId)).thenReturn(Optional.of(mockVerification));
        when(paymentDisputeRepository.save(any(PaymentDispute.class))).thenReturn(mockDispute);
        doNothing().when(webhookPublisher).publish(any(EventType.class), anyString());

        // Act
        PaymentDisputeResponse result = paymentDisputeService.raise(disputeRequest, raisedByUserId);

        // Assert
        assertNotNull(result);
        
        ArgumentCaptor<PaymentDispute> disputeCaptor = ArgumentCaptor.forClass(PaymentDispute.class);
        verify(paymentDisputeRepository, times(1)).save(disputeCaptor.capture());
        
        PaymentDispute savedDispute = disputeCaptor.getValue();
        assertEquals(PaymentDisputeStage.STAFF_REVIEW, savedDispute.getStage());
        assertEquals(verificationId, savedDispute.getVerification().getId());
        assertEquals(raisedByUserId, savedDispute.getRaisedByUserId());
    }
}
