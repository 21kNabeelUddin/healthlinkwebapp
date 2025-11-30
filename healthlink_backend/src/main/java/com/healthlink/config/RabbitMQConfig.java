package com.healthlink.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration
 * Sets up exchanges, queues, and bindings for async messaging
 */
@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String HEALTHLINK_EXCHANGE = "healthlink.exchange";
    public static final String HEALTHLINK_DLX = "healthlink.dlx"; // Dead letter exchange

    // Queue names
    public static final String NOTIFICATION_QUEUE = "healthlink.notifications";
    public static final String EMAIL_QUEUE = "healthlink.emails";
    public static final String SMS_QUEUE = "healthlink.sms";
    public static final String WEBHOOK_QUEUE = "healthlink.webhooks";
    public static final String ANALYTICS_QUEUE = "healthlink.analytics";

    // Routing keys
    public static final String NOTIFICATION_ROUTING_KEY = "notification.#";
    public static final String EMAIL_ROUTING_KEY = "email.#";
    public static final String SMS_ROUTING_KEY = "sms.#";
    public static final String WEBHOOK_ROUTING_KEY = "webhook.#";
    public static final String ANALYTICS_ROUTING_KEY = "analytics.#";

    /**
     * Main exchange for all healthlink events
     */
    @Bean
    public TopicExchange healthlinkExchange() {
        return new TopicExchange(HEALTHLINK_EXCHANGE, true, false);
    }

    /**
     * Dead letter exchange for failed messages
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(HEALTHLINK_DLX, true, false);
    }

    /**
     * Notification queue
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", HEALTHLINK_DLX)
                .build();
    }

    /**
     * Email queue
     */
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", HEALTHLINK_DLX)
                .build();
    }

    /**
     * SMS queue
     */
    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE)
                .withArgument("x-dead-letter-exchange", HEALTHLINK_DLX)
                .build();
    }

    /**
     * Webhook queue
     */
    @Bean
    public Queue webhookQueue() {
        return QueueBuilder.durable(WEBHOOK_QUEUE)
                .withArgument("x-dead-letter-exchange", HEALTHLINK_DLX)
                .build();
    }

    /**
     * Analytics queue
     */
    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE)
                .withArgument("x-dead-letter-exchange", HEALTHLINK_DLX)
                .build();
    }

    /**
     * Bindings
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(healthlinkExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue())
                .to(healthlinkExchange())
                .with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding smsBinding() {
        return BindingBuilder.bind(smsQueue())
                .to(healthlinkExchange())
                .with(SMS_ROUTING_KEY);
    }

    @Bean
    public Binding webhookBinding() {
        return BindingBuilder.bind(webhookQueue())
                .to(healthlinkExchange())
                .with(WEBHOOK_ROUTING_KEY);
    }

    @Bean
    public Binding analyticsBinding() {
        return BindingBuilder.bind(analyticsQueue())
                .to(healthlinkExchange())
                .with(ANALYTICS_ROUTING_KEY);
    }

    /**
     * JSON message converter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * Listener container factory with JSON converter
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }
}
