package com.healthlink.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * RabbitMQ configuration for async workers (webhooks, notifications).
 * Only active when 'rabbitmq' profile is enabled.
 */
@Configuration
@Profile("rabbitmq")
@RequiredArgsConstructor
public class RabbitMQConfig {

    public static final String WEBHOOK_QUEUE = "healthlink.webhooks";
    public static final String WEBHOOK_EXCHANGE = "healthlink.webhooks.exchange";
    public static final String WEBHOOK_ROUTING_KEY = "webhook.delivery";
    public static final String WEBHOOK_DLQ = "healthlink.webhooks.dlq";
    public static final String WEBHOOK_DLX = "healthlink.webhooks.dlx";

    public static final String NOTIFICATION_QUEUE = "healthlink.notifications";
    public static final String NOTIFICATION_EXCHANGE = "healthlink.notifications.exchange";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.send";
    public static final String NOTIFICATION_DLQ = "healthlink.notifications.dlq";
    public static final String NOTIFICATION_DLX = "healthlink.notifications.dlx";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // Webhook Queue and Exchange
    @Bean
    public Queue webhookQueue() {
        return QueueBuilder.durable(WEBHOOK_QUEUE)
                .withArgument("x-dead-letter-exchange", WEBHOOK_DLX)
                .withArgument("x-dead-letter-routing-key", "webhook.failed")
                .build();
    }

    @Bean
    public DirectExchange webhookExchange() {
        return new DirectExchange(WEBHOOK_EXCHANGE);
    }

    @Bean
    public Binding webhookBinding(Queue webhookQueue, DirectExchange webhookExchange) {
        return BindingBuilder.bind(webhookQueue).to(webhookExchange).with(WEBHOOK_ROUTING_KEY);
    }

    @Bean
    public Queue webhookDLQ() {
        return QueueBuilder.durable(WEBHOOK_DLQ).build();
    }

    @Bean
    public DirectExchange webhookDLX() {
        return new DirectExchange(WEBHOOK_DLX);
    }

    @Bean
    public Binding webhookDLQBinding(Queue webhookDLQ, DirectExchange webhookDLX) {
        return BindingBuilder.bind(webhookDLQ).to(webhookDLX).with("webhook.failed");
    }

    // Notification Queue and Exchange
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", "notification.failed")
                .build();
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Queue notificationDLQ() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public DirectExchange notificationDLX() {
        return new DirectExchange(NOTIFICATION_DLX);
    }

    @Bean
    public Binding notificationDLQBinding(Queue notificationDLQ, DirectExchange notificationDLX) {
        return BindingBuilder.bind(notificationDLQ).to(notificationDLX).with("notification.failed");
    }
}
