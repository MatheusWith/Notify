package com.notify.newsletter.infrastructure.messaging;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record CampaignPublishedMessage(UUID messageId, UUID campaignId, UUID newsletterId, String newsletterSlug,
        String subject, String content, List<String> subscriberEmails) implements Serializable {

    public CampaignPublishedMessage {
        subscriberEmails = List.copyOf(subscriberEmails);
    }
}
