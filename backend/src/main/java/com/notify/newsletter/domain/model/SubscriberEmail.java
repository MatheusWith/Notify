package com.notify.newsletter.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;

@Embeddable
public record SubscriberEmail(@Column(name = "email", nullable = false) String value) {

    private static final String PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    public SubscriberEmail {
        if (value != null) {
            value = value.trim().toLowerCase(Locale.ENGLISH);
        }
        if (value == null || !value.matches(PATTERN)) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }
}
