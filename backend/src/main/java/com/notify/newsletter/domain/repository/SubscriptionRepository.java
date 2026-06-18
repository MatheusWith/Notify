package com.notify.newsletter.domain.repository;

import com.notify.newsletter.domain.model.SubscriberEmail;
import com.notify.newsletter.domain.model.Subscription;
import com.notify.newsletter.domain.model.SubscriptionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubscriptionRepository {

    Optional<Subscription> findById(UUID id);

    Subscription save(Subscription subscription);

    Optional<Subscription> findByEmail(SubscriberEmail email);

    Optional<Subscription> findByToken(UUID token);

    Optional<Subscription> findByEmailAndStatus(SubscriberEmail email, SubscriptionStatus status);

    long countByNewsletterIdAndStatus(UUID newsletterId, SubscriptionStatus status);

    Page<Subscription> findByNewsletterId(UUID newsletterId, Pageable pageable);
}
