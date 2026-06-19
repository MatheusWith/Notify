package com.notify.identity.domain.model;

import com.notify.shared.domain.InvalidPasswordException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record PasswordHash(@Column(name = "password", nullable = false) String value) {

    private static final String BCRYPT_PATTERN = "^\\$2[abdy]\\$\\d{2}\\$[A-Za-z0-9./]{53}$";

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new InvalidPasswordException("Password hash must not be null or blank");
        }
        if (!value.matches(BCRYPT_PATTERN)) {
            throw new InvalidPasswordException("Invalid BCrypt hash format");
        }
    }
}
