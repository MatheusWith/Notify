package com.notify.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record UserId(@Column(columnDefinition = "bigint") Long value) {

    public UserId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("UserId must be a positive number");
        }
    }

    public static UserId of(Long value) {
        return new UserId(value);
    }
}
