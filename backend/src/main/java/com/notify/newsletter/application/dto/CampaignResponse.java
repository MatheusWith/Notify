package com.notify.newsletter.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CampaignResponse(UUID id, UUID newsletterId, String subject, String content, String status,
        LocalDateTime scheduledAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
