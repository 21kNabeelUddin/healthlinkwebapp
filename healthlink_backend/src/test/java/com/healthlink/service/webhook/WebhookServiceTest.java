package com.healthlink.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for WebhookService
 */
@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookService webhookService;

    private WebhookService.WebhookSubscription testSubscription;
    private WebhookService.WebhookEvent testEvent;

    @BeforeEach
    void setUp() {
        testSubscription = WebhookService.WebhookSubscription.builder()
                .id("sub-1")
                .url("https://example.com/webhook")
                .secret("test-secret")
                .subscribedEvents(Set.of("appointment.created", "payment.verified"))
                .build();

        testEvent = WebhookService.WebhookEvent.builder()
                .eventType("appointment.created")
                .entityId("appt-123")
                .entityType("Appointment")
                .data(Map.of("doctorId", "doc-456", "patientId", "pat-789"))
                .build();

        webhookService.registerSubscription(testSubscription);
    }

    @Test
    void emitEvent_shouldSendWebhookToSubscribers() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        webhookService.emitEvent(testEvent);

        // Give async execution time to complete
        Thread.sleep(100);

        verify(restTemplate).postForEntity(
                eq("https://example.com/webhook"),
                any(HttpEntity.class),
                eq(String.class));
    }

    @Test
    void emitEvent_shouldIncludeHmacSignature() throws Exception {
        ArgumentCaptor<HttpEntity<?>> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
        when(restTemplate.postForEntity(anyString(), requestCaptor.capture(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        webhookService.emitEvent(testEvent);

        Thread.sleep(100);

        HttpEntity<?> capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getHeaders().get("X-Webhook-Signature")).isNotNull();
        assertThat(capturedRequest.getHeaders().get("X-Webhook-Event-Type"))
                .containsExactly("appointment.created");
    }

    @Test
    void emitEvent_shouldNotSendToUnsubscribedEvents() throws Exception {
        WebhookService.WebhookEvent unsubscribedEvent = WebhookService.WebhookEvent.builder()
                .eventType("prescription.created")
                .entityId("presc-123")
                .entityType("Prescription")
                .data(Map.of())
                .build();

        webhookService.emitEvent(unsubscribedEvent);

        Thread.sleep(100);

        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    void emitEvent_shouldHandleFailureGracefully() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        // Should not throw exception
        assertThatCode(() -> webhookService.emitEvent(testEvent)).doesNotThrowAnyException();

        Thread.sleep(100);

        verify(restTemplate).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void registerSubscription_shouldStoreSubscription() {
        WebhookService.WebhookSubscription newSubscription = WebhookService.WebhookSubscription.builder()
                .id("sub-2")
                .url("https://another.com/webhook")
                .secret("another-secret")
                .subscribedEvents(Set.of("payment.initiated"))
                .build();

        assertThatCode(() -> webhookService.registerSubscription(newSubscription))
                .doesNotThrowAnyException();
    }

    @Test
    void eventTypes_shouldHavePredefinedConstants() {
        assertThat(WebhookService.EventTypes.APPOINTMENT_CREATED).isEqualTo("appointment.created");
        assertThat(WebhookService.EventTypes.PAYMENT_VERIFIED).isEqualTo("payment.verified");
        assertThat(WebhookService.EventTypes.USER_APPROVED).isEqualTo("user.approved");
    }
}
