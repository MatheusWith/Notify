package com.notify.newsletter.infrastructure.messaging;

import com.notify.newsletter.domain.model.Subscription;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsletterEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishConfirmationEmail(Subscription subscription) {
        ConfirmationEmailMessage message = new ConfirmationEmailMessage(UUID.randomUUID(), subscription.getId(),
                subscription.getEmail().value(), subscription.getToken(), subscription.getExpiresAt());

        rabbitTemplate.convertAndSend(NewsletterRabbitMQConfig.EXCHANGE, NewsletterRabbitMQConfig.ROUTING_KEY, message,
                msg -> {
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    msg.getMessageProperties().setMessageId(message.messageId().toString());
                    msg.getMessageProperties().setContentType("application/json");
                    return msg;
                });
    }
}
