package com.notify.identity.domain.event;

import com.notify.identity.domain.model.UserId;
import com.notify.shared.domain.event.DomainEvent;
import java.time.LocalDateTime;

public record UserRegistered(UserId userId, String email, String name,
        LocalDateTime occurredOn) implements DomainEvent {

    public UserRegistered(UserId userId, String email, String name) {
        this(userId, email, name, LocalDateTime.now());
    }
}
