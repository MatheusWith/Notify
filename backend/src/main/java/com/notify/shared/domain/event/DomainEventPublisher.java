package com.notify.shared.domain.event;

@FunctionalInterface
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
