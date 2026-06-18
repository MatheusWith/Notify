package com.notify.shared.domain.event;

import java.time.LocalDateTime;

@FunctionalInterface
public interface DomainEvent {

    LocalDateTime occurredOn();
}
