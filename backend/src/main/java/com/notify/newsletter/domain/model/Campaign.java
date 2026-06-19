package com.notify.newsletter.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Campaign {

    private UUID id;

    private UUID newsletterId;

    private String subject;

    private String content;

    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    private LocalDateTime scheduledAt;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void submit() {
        if (status != CampaignStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT campaigns can be submitted");
        }
        this.status = CampaignStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
    }

    public void publish() {
        if (status != CampaignStatus.DRAFT && status != CampaignStatus.PENDING) {
            throw new IllegalStateException("Only DRAFT or PENDING campaigns can be published");
        }
        this.status = CampaignStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markSent() {
        if (status != CampaignStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED campaigns can be marked as sent");
        }
        this.status = CampaignStatus.SENT;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isEditable() {
        return status == CampaignStatus.DRAFT || status == CampaignStatus.PENDING;
    }

    public boolean isDeletable() {
        return status == CampaignStatus.DRAFT || status == CampaignStatus.PENDING;
    }

    public void updateContent(String subject, String content, LocalDateTime scheduledAt) {
        if (!isEditable()) {
            throw new IllegalStateException("Cannot update a campaign with status " + status);
        }
        this.subject = subject;
        this.content = content;
        this.scheduledAt = scheduledAt;
        this.updatedAt = LocalDateTime.now();
    }
}
