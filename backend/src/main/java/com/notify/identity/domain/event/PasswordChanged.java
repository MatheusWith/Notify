package com.notify.identity.domain.event;

import com.notify.identity.domain.model.UserId;
import com.notify.shared.domain.event.DomainEvent;
import java.time.LocalDateTime;

public record PasswordChanged(UserId userId, LocalDateTime occurredOn) implements DomainEvent {

    public PasswordChanged(UserId userId) {
        this(userId, LocalDateTime.now());
    }
}
