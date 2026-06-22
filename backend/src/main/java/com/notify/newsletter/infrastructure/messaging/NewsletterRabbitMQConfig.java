package com.notify.newsletter.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NewsletterRabbitMQConfig {

    public static final String EXCHANGE = "newsletter.direct";
    public static final String DLX = "newsletter.dlx";
    public static final String CONFIRMATION_QUEUE = "newsletter.subscription.confirmation.queue";
    public static final String CONFIRMATION_DLQ = "newsletter.subscription.confirmation.dlq";
    public static final String ROUTING_KEY = "newsletter.subscription.confirmation";
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
        return BindingBuilder.bind(confirmationQueue()).to(newsletterExchange()).with(ROUTING_KEY);
    }

    @Bean
    public Queue campaignQueue() {
        return QueueBuilder.durable(CAMPAIGN_QUEUE).withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", CAMPAIGN_DLQ).build();
    }

    @Bean
    public Queue campaignDeadLetterQueue() {
        return QueueBuilder.durable(CAMPAIGN_DLQ).build();
    }

    @Bean
    public Binding campaignBinding() {
        return BindingBuilder.bind(campaignQueue()).to(newsletterExchange()).with(CAMPAIGN_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
