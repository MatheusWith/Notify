package com.notify.newsletter.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateCampaignRequest(
        @NotBlank(message = "Subject is required") @Size(max = 200, message = "Subject must be at most 200 characters") String subject,

        @NotBlank(message = "Content is required") @Size(max = 20000, message = "Content must be at most 20000 characters") String content,

        LocalDateTime scheduledAt) {
}
