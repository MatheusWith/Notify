package com.notify.worker.email.domain.model;

import com.notify.worker.shared.domain.BusinessException;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public record CampaignPublishedMessage(UUID messageId, UUID campaignId, UUID newsletterId, String newsletterSlug,
        String subject, String content, List<String> subscriberEmails) implements Serializable {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public CampaignPublishedMessage {
        subscriberEmails = List.copyOf(subscriberEmails);
        if (messageId == null) {
            throw new BusinessException(400, "messageId must not be null");
        }
        if (campaignId == null) {
            throw new BusinessException(400, "campaignId must not be null");
        }
        if (newsletterId == null) {
            throw new BusinessException(400, "newsletterId must not be null");
        }
        if (newsletterSlug == null || newsletterSlug.isBlank()) {
            throw new BusinessException(400, "newsletterSlug must not be null or blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new BusinessException(400, "subject must not be null or blank");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(400, "content must not be null or blank");
        }
        if (subscriberEmails == null || subscriberEmails.isEmpty()) {
            throw new BusinessException(400, "subscriberEmails must not be null or empty");
        }
        for (String email : subscriberEmails) {
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                throw new BusinessException(400, "invalid subscriber email format: " + email);
            }
        }
    }
}
