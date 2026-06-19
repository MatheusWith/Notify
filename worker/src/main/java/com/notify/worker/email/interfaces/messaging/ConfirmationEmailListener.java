package com.notify.worker.email.interfaces.messaging;

import com.notify.worker.email.application.port.in.ConsumeEmailUseCase;
import com.notify.worker.email.domain.model.ConfirmationEmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfirmationEmailListener {

    private final ConsumeEmailUseCase consumeEmailUseCase;

    @RabbitListener(queues = "${app.rabbitmq.queue.confirmation}", concurrency = "${app.rabbitmq.listener.concurrency}")
    public void handleConfirmationEmail(ConfirmationEmailMessage message) {
        if (message == null) {
            log.warn("Received null confirmation email message");
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("Received confirmation email message: {}", message.messageId());
        }
        consumeEmailUseCase.process(message);
    }
}
