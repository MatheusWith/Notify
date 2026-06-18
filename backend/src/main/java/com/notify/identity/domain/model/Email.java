package com.notify.identity.domain.model;

import com.notify.shared.domain.InvalidEmailException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;

@Embeddable
public record Email(@Column(name = "email", nullable = false, unique = true) String value) {

    private static final String PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    public Email {
        if (value != null) {
            value = value.trim().toLowerCase(Locale.ENGLISH);
        }
        if (value == null || !value.matches(PATTERN)) {
            throw new InvalidEmailException("Invalid email format: " + value);
        }
    }
}
