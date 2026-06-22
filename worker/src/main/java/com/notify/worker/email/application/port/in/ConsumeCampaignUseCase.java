package com.notify.worker.email.application.port.in;

import com.notify.worker.email.domain.model.CampaignPublishedMessage;

@FunctionalInterface
public interface ConsumeCampaignUseCase {
    void process(CampaignPublishedMessage message);
}
