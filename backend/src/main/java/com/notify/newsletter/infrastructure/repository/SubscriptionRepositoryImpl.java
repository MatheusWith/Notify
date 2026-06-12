package com.notify.newsletter.infrastructure.repository;

import com.notify.newsletter.domain.model.SubscriberEmail;
import com.notify.newsletter.domain.model.Subscription;
import com.notify.newsletter.domain.model.SubscriptionStatus;
import com.notify.newsletter.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final JpaSubscriptionRepository jpaSubscriptionRepository;
    private final SubscriptionDomainMapper mapper;

    @Override
    public Optional<Subscription> findById(UUID id) {
        return jpaSubscriptionRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Subscription save(Subscription subscription) {
        JpaSubscriptionEntity entity = mapper.toJpa(subscription);
        entity = jpaSubscriptionRepository.save(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Subscription> findByEmail(SubscriberEmail email) {
        return jpaSubscriptionRepository.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Subscription> findByToken(UUID token) {
        return jpaSubscriptionRepository.findByToken(token).map(mapper::toDomain);
    }

    @Override
    public Optional<Subscription> findByEmailAndStatus(SubscriberEmail email, SubscriptionStatus status) {
        return jpaSubscriptionRepository.findByEmailAndStatus(email.value(), status.name())
                .map(mapper::toDomain);
    }

    @Override
    public long countByNewsletterIdAndStatus(UUID newsletterId, SubscriptionStatus status) {
        return jpaSubscriptionRepository.countByNewsletterIdAndStatus(newsletterId, status.name());
    }

    @Override
    public Page<Subscription> findByNewsletterId(UUID newsletterId, Pageable pageable) {
        return jpaSubscriptionRepository.findByNewsletterId(newsletterId, pageable)
                .map(mapper::toDomain);
    }
}
