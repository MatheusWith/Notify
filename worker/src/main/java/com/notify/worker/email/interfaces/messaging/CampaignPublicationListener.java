package com.notify.worker.email.interfaces.messaging;

import com.notify.worker.email.application.port.in.ConsumeCampaignUseCase;
import com.notify.worker.email.domain.model.CampaignPublishedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignPublicationListener {

    private final ConsumeCampaignUseCase consumeCampaignUseCase;

    @RabbitListener(queues = "newsletter.campaign.send.queue")
    public void handleCampaignPublication(CampaignPublishedMessage message) {
        if (message == null) {
            log.warn("Received null campaign publication message");
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("Received campaign publication message: {} - '{}'", message.campaignId(), message.subject());
        }
        consumeCampaignUseCase.process(message);
    }
}
