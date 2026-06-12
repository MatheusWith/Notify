package com.notify.newsletter.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class SubscriptionTest {

    @Test
    void givenValidData_whenBuilt_thenSucceeds() {
        SubscriberEmail email = new SubscriberEmail("user@example.com");
        Subscription subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .email(email)
                .newsletterId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        assertThat(subscription.getId()).isNotNull();
        assertThat(subscription.getEmail()).isEqualTo(email);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(subscription.getToken()).isNotNull();
        assertThat(subscription.getExpiresAt()).isNotNull();
        assertThat(subscription.getCreatedAt()).isNotNull();
        assertThat(subscription.getUpdatedAt()).isNotNull();
    }

    @Test
    void givenNoSubscriberId_whenBuilt_thenSubscriberIdIsNull() {
        Subscription subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .email(new SubscriberEmail("user@example.com"))
                .newsletterId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        assertThat(subscription.getSubscriberId()).isNull();
    }

    @Test
    void givenSubscriberId_whenBuilt_thenReturnsSubscriberId() {
        Long subscriberId = 42L;
        Subscription subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .email(new SubscriberEmail("user@example.com"))
                .newsletterId(UUID.randomUUID())
                .subscriberId(subscriberId)
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        assertThat(subscription.getSubscriberId()).isEqualTo(subscriberId);
    }

    @Test
    void givenExpiredToken_whenIsExpired_thenReturnsTrue() {
        Subscription subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .email(new SubscriberEmail("user@example.com"))
                .newsletterId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        assertThat(subscription.isExpired()).isTrue();
    }

    @Test
    void givenValidToken_whenIsExpired_thenReturnsFalse() {
        Subscription subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .email(new SubscriberEmail("user@example.com"))
                .newsletterId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        assertThat(subscription.isExpired()).isFalse();
    }
}
