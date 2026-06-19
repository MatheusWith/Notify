package com.notify.newsletter.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.notify.newsletter.domain.model.SubscriberEmail;
import com.notify.newsletter.domain.model.Subscription;
import com.notify.newsletter.domain.model.SubscriptionStatus;
import com.notify.newsletter.domain.repository.SubscriptionRepository;
import com.notify.newsletter.interfaces.AbstractNewsletterIntegrationTest;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

class SubscriptionRepositoryTest extends AbstractNewsletterIntegrationTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    void givenExistingNewsletter_whenFindByNewsletterId_thenReturnsSubscriptions() {
        UUID newsletterId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

        Subscription sub1 = subscriptionRepository.save(createSubscription(newsletterId, "sub1@example.com", 42L));
        Subscription sub2 = subscriptionRepository.save(createSubscription(newsletterId, "sub2@example.com", 43L));
        subscriptionRepository.save(createSubscription(UUID.randomUUID(), "other@example.com", null));

        Page<Subscription> result = subscriptionRepository.findByNewsletterId(newsletterId, PageRequest.of(0, 10));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Subscription::getId).containsExactlyInAnyOrder(sub1.getId(), sub2.getId());
    }

    @Test
    void givenNewsletterWithNoSubscriptions_whenFindByNewsletterId_thenReturnsEmptyPage() {
        UUID newsletterId = UUID.randomUUID();

        Page<Subscription> result = subscriptionRepository.findByNewsletterId(newsletterId, PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    @Test
    void givenMultipleSubscriptions_whenFindByNewsletterId_thenSupportsPagination() {
        UUID newsletterId = UUID.randomUUID();

        for (int i = 0; i < 5; i++) {
            subscriptionRepository.save(createSubscription(newsletterId, "page" + i + "@example.com", 100L + i));
        }

        Page<Subscription> firstPage = subscriptionRepository.findByNewsletterId(newsletterId, PageRequest.of(0, 2));
        assertThat(firstPage).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
    }

    @Test
    void givenSubscriberIdNotNull_whenFindByNewsletterId_thenSubscriberIdIsPreserved() {
        UUID newsletterId = UUID.randomUUID();
        Long subscriberId = 99L;

        Subscription saved = subscriptionRepository
                .save(createSubscription(newsletterId, "with-id@example.com", subscriberId));

        Page<Subscription> result = subscriptionRepository.findByNewsletterId(newsletterId, PageRequest.of(0, 10));
        assertThat(result).hasSize(1);

        Subscription found = result.getContent().get(0);
        assertThat(found.getSubscriberId()).isEqualTo(subscriberId);
    }

    private Subscription createSubscription(UUID newsletterId, String email, Long subscriberId) {
        return Subscription.builder().id(UUID.randomUUID()).email(new SubscriberEmail(email)).newsletterId(newsletterId)
                .subscriberId(subscriberId).token(UUID.randomUUID()).expiresAt(LocalDateTime.now().plusHours(24))
                .status(SubscriptionStatus.CONFIRMED).build();
    }
}
