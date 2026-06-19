package com.notify.worker.email.application.port.out;

import com.notify.worker.email.domain.model.EmailDeliveryResult;
import com.notify.worker.email.domain.model.EmailMessage;

@FunctionalInterface
public interface SendEmailPort {
    EmailDeliveryResult send(EmailMessage message);
}
