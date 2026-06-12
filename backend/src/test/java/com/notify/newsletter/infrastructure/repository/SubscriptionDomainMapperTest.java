package com.notify.newsletter.infrastructure.repository;

import com.notify.newsletter.domain.model.SubscriberEmail;
import com.notify.newsletter.domain.model.Subscription;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionDomainMapperTest {

    private final SubscriptionDomainMapper mapper = new SubscriptionDomainMapper();

    @Test
    void givenSubscriptionWithSubscriberId_whenMappedToJpaAndBack_thenPreservesSubscriberId() {
        Long subscriberId = 42L;
        Subscription domain = createSubscription(subscriberId);

        JpaSubscriptionEntity entity = mapper.toJpa(domain);
        Subscription result = mapper.toDomain(entity);

        assertThat(result.getSubscriberId()).isEqualTo(subscriberId);
    }

    @Test
    void givenSubscriptionWithoutSubscriberId_whenMappedToJpaAndBack_thenSubscriberIdIsNull() {
        Subscription domain = createSubscription(null);

        JpaSubscriptionEntity entity = mapper.toJpa(domain);
        Subscription result = mapper.toDomain(entity);

        assertThat(result.getSubscriberId()).isNull();
    }

    private Subscription createSubscription(Long subscriberId) {
        return Subscription.builder()
                .id(UUID.randomUUID())
                .email(new SubscriberEmail("user@example.com"))
                .newsletterId(UUID.randomUUID())
                .subscriberId(subscriberId)
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }
}
