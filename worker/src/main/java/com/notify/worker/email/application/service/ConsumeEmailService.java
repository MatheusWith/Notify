package com.notify.worker.email.application.service;

import com.notify.worker.email.application.port.in.ConsumeEmailUseCase;
import com.notify.worker.email.application.port.out.SendEmailPort;
import com.notify.worker.email.domain.model.EmailDeliveryResult;
import com.notify.worker.email.domain.model.EmailMessage;
import com.notify.worker.email.domain.service.EmailSendingService;
import com.notify.worker.shared.domain.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ConsumeEmailService implements ConsumeEmailUseCase {

    private final SendEmailPort sendEmailPort;
    private final EmailSendingService emailSendingService;

    @Override
    public void process(EmailMessage message) {
        if (log.isInfoEnabled()) {
            log.info("Processing email message: {}", message.messageId());
        }

        // Zero Trust: revalida email em camada de aplicação
        emailSendingService.validateEmail(message.recipientEmail());

        var content = message.body();
        if (content == null || content.isBlank()) {
            throw new BusinessException(400, "email content must not be empty");
        }

        try {
            EmailDeliveryResult result = sendEmailPort.send(message);
            if (!result.success()) {
                if (log.isErrorEnabled()) {
                    log.error("Failed to send email {}: {}", message.messageId(), result.errorMessage());
                }
                throw new BusinessException(500, "Failed to send email: " + result.errorMessage());
            }
            if (log.isInfoEnabled()) {
                log.info("Email sent successfully: {}", message.messageId());
            }
        } catch (BusinessException e) {
            if (log.isWarnEnabled()) {
                log.warn("Send attempt failed for message {}: {}", message.messageId(), e.getMessage());
            }
            throw e;
        }
    }
}
