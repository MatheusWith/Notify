package com.notify.newsletter.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;

@Embeddable
public record Slug(@Column(unique = true, nullable = false, length = 100) String value) {

    private static final String PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$";

    public Slug {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Slug must not be null or blank");
        }
        value = value.trim().toLowerCase(Locale.ENGLISH);
        if (!value.matches(PATTERN)) {
            throw new IllegalArgumentException(
                    "Invalid slug format: '" + value + "'. Use only lowercase letters, numbers, and hyphens.");
        }
    }
}
