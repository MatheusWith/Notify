package com.notify.worker.email.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmailDeliveryResult(UUID messageId, boolean success, String recipient, String errorMessage,
        LocalDateTime attemptedAt) {

    public EmailDeliveryResult {
        if (messageId == null) {
            throw new IllegalArgumentException("messageId must not be null");
        }
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipient must not be null or blank");
        }
        if (attemptedAt == null) {
            throw new IllegalArgumentException("attemptedAt must not be null");
        }
    }

    public static EmailDeliveryResult success(UUID messageId, String recipient) {
        return new EmailDeliveryResult(messageId, true, recipient, null, LocalDateTime.now());
    }

    public static EmailDeliveryResult failure(UUID messageId, String recipient, String error) {
        return new EmailDeliveryResult(messageId, false, recipient, error, LocalDateTime.now());
    }
}
