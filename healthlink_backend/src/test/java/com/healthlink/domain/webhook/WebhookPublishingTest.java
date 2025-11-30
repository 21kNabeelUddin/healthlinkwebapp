package com.healthlink.domain.webhook;

import com.healthlink.domain.webhook.entity.PublishedEvent;
import com.healthlink.domain.webhook.entity.WebhookSubscription;
import com.healthlink.domain.webhook.repository.PublishedEventRepository;
import com.healthlink.domain.webhook.repository.WebhookSubscriptionRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookPublishingTest {

    @Mock
    private WebhookSubscriptionRepository subscriptionRepository;

    @Mock
    private PublishedEventRepository eventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private WebhookPublisherService webhookPublisherService;

    @Test
    void shouldPublishEventWhenSubscriptionExists() {
        // Given
        EventType eventType = EventType.APPOINTMENT_CREATED;
        String referenceId = "appt-123";
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setEventType(eventType);
        subscription.setTargetUrl("https://example.com/webhook");
        subscription.setActive(true);
        subscription.setId(UUID.randomUUID());

        when(subscriptionRepository.findByEventTypeAndActiveTrue(eventType))
                .thenReturn(List.of(subscription));
        when(eventRepository.save(any(PublishedEvent.class))).thenAnswer(invocation -> {
            PublishedEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        // When
        webhookPublisherService.publish(eventType, referenceId);

        // Then
        verify(eventRepository).save(any(PublishedEvent.class));
    }
}
