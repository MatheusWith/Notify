package com.notify.newsletter.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConfirmSubscriptionRequest(@NotNull(message = "Token is required") UUID token) {
}
