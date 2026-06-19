package com.notify.worker.email.application.port.in;

import com.notify.worker.email.domain.model.EmailMessage;

@FunctionalInterface
public interface ConsumeEmailUseCase {
    void process(EmailMessage message);
}
