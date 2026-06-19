package com.notify.newsletter.infrastructure.messaging;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record ConfirmationEmailMessage(UUID messageId, UUID subscriptionId, String email, UUID token,
        LocalDateTime expiresAt) implements Serializable {
}
