package com.notify.worker.email.domain.service;

import com.notify.worker.shared.domain.BusinessException;
import java.util.regex.Pattern;

public class EmailSendingService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(400, "email must not be null or blank");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(400, "invalid email format: " + email);
        }
    }

    public String buildConfirmationContent(String token, String expiresAt) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(400, "token must not be null or blank");
        }
        return "<html><body><h1>Confirm your subscription</h1>"
                + "<p>Click <a href='http://localhost:8080/api/v1/newsletter/confirm?token=" + token + "'>here</a> "
                + "to confirm your subscription.</p>" + "<p>This link expires at: "
                + (expiresAt != null ? expiresAt : "unknown") + "</p>" + "</body></html>";
    }

    public String buildSubject() {
        return "Confirm your subscription to Notify";
    }
}
