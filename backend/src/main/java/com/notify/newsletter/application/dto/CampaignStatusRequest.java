package com.notify.newsletter.application.dto;

import com.notify.newsletter.domain.model.CampaignStatus;
import jakarta.validation.constraints.NotNull;

public record CampaignStatusRequest(@NotNull(message = "Status is required") CampaignStatus status) {
}
