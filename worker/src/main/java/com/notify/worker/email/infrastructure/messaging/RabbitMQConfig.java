package com.notify.worker.email.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "newsletter.direct";
    public static final String DLX = "newsletter.dlx";
    public static final String CONFIRMATION_QUEUE = "newsletter.subscription.confirmation.queue";
    public static final String CONFIRMATION_DLQ = "newsletter.subscription.confirmation.dlq";
    public static final String CONFIRMATION_ROUTING_KEY = "newsletter.subscription.confirmation";

    // Campaign queue — preparado para uso futuro
    public static final String CAMPAIGN_QUEUE = "newsletter.campaign.send.queue";
    public static final String CAMPAIGN_DLQ = "newsletter.campaign.send.dlq";
    public static final String CAMPAIGN_ROUTING_KEY = "newsletter.campaign.send";

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
        return BindingBuilder.bind(confirmationQueue()).to(newsletterExchange()).with(CONFIRMATION_ROUTING_KEY);
    }
}
