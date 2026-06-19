package com.notify.newsletter.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NewsletterRabbitMQConfig {

    public static final String EXCHANGE = "newsletter.direct";
    public static final String DLX = "newsletter.dlx";
    public static final String CONFIRMATION_QUEUE = "newsletter.subscription.confirmation.queue";
    public static final String CONFIRMATION_DLQ = "newsletter.subscription.confirmation.dlq";
    public static final String ROUTING_KEY = "newsletter.subscription.confirmation";

    @Bean
    public DirectExchange newsletterExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange newsletterDeadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX).durable(true).build();
    }

    @Bean
    public Queue confirmationQueue() {
        return QueueBuilder.durable(CONFIRMATION_QUEUE).withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", CONFIRMATION_DLQ).build();
    }

    @Bean
    public Queue confirmationDeadLetterQueue() {
        return QueueBuilder.durable(CONFIRMATION_DLQ).build();
    }

    @Bean
    public Binding confirmationBinding() {
        return BindingBuilder.bind(confirmationQueue()).to(newsletterExchange()).with(ROUTING_KEY);
    }
}
