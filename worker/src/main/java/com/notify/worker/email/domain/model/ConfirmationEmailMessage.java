package com.notify.worker.email.domain.model;

import com.notify.worker.shared.domain.BusinessException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record ConfirmationEmailMessage(UUID messageId, UUID subscriptionId, String email, UUID token,
        LocalDateTime expiresAt) implements EmailMessage, Serializable {

    public ConfirmationEmailMessage {
        if (messageId == null) {
            throw new BusinessException(400, "messageId must not be null");
        }
        if (subscriptionId == null) {
            throw new BusinessException(400, "subscriptionId must not be null");
        }
        if (email == null || email.isBlank()) {
            throw new BusinessException(400, "email must not be null or blank");
        }
        if (token == null) {
            throw new BusinessException(400, "token must not be null");
        }
        if (expiresAt == null) {
            throw new BusinessException(400, "expiresAt must not be null");
        }
    }

    @Override
    public String recipientEmail() {
        return email;
    }

    @Override
    public String subject() {
        return "Confirm your subscription to Notify";
    }

    @Override
    public String body() {
        return "<html><body><h1>Confirm your subscription</h1>"
                + "<p>Click <a href='http://localhost:8080/api/v1/newsletter/confirm?token=" + token + "'>here</a> "
                + "to confirm your subscription.</p>" + "<p>This link expires at: " + expiresAt + "</p>"
                + "</body></html>";
    }
}
