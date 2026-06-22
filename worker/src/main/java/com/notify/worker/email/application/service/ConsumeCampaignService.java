package com.notify.worker.email.application.service;

import com.notify.worker.email.application.port.in.ConsumeCampaignUseCase;
import com.notify.worker.email.application.port.out.SendEmailPort;
import com.notify.worker.email.domain.model.CampaignPublishedMessage;
import com.notify.worker.email.domain.model.EmailDeliveryResult;
import com.notify.worker.email.domain.model.EmailMessage;
import com.notify.worker.email.domain.service.EmailSendingService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumeCampaignService implements ConsumeCampaignUseCase {

    private final SendEmailPort sendEmailPort;
    private final EmailSendingService emailSendingService;

    @Override
    public void process(CampaignPublishedMessage message) {
        var subscriberEmails = message.subscriberEmails();
        if (subscriberEmails == null || subscriberEmails.isEmpty()) {
            if (log.isWarnEnabled()) {
                log.warn("Campaign {} has no subscribers to send to", message.campaignId());
            }
            return;
        }

        if (log.isInfoEnabled()) {
            log.info("Processing campaign publication '{}' for {} subscribers", message.subject(),
                    subscriberEmails.size());
        }

        for (String email : subscriberEmails) {
            try {
                emailSendingService.validateEmail(email);

                if (log.isDebugEnabled()) {
                    log.debug("Sending campaign '{}' to {}", message.subject(), email);
                }

                CampaignEmailMessage emailMessage = new CampaignEmailMessage(UUID.randomUUID(), email,
                        message.subject(), message.content());
                EmailDeliveryResult result = sendEmailPort.send(emailMessage);

                if (result.success()) {
                    if (log.isInfoEnabled()) {
                        log.info("Campaign email sent to {}: {}", email, result.messageId());
                    }
                } else {
                    if (log.isErrorEnabled()) {
                        log.error("Failed to send campaign email to {}: {}", email, result.errorMessage());
                    }
                }
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Error sending campaign email to {}: {}", email, e.getMessage());
                }
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Finished processing campaign publication '{}'", message.subject());
        }
    }

    private record CampaignEmailMessage(UUID messageId, String recipientEmail, String subject,
            String body) implements EmailMessage {
    }
}
