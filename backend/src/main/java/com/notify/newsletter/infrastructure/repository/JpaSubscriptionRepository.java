package com.notify.newsletter.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSubscriptionRepository extends JpaRepository<JpaSubscriptionEntity, UUID> {

    Optional<JpaSubscriptionEntity> findByEmail(String email);

    Optional<JpaSubscriptionEntity> findByToken(UUID token);

    Optional<JpaSubscriptionEntity> findByEmailAndStatus(String email, String status);

    long countByNewsletterIdAndStatus(UUID newsletterId, String status);

    Page<JpaSubscriptionEntity> findByNewsletterId(UUID newsletterId, Pageable pageable);
}
