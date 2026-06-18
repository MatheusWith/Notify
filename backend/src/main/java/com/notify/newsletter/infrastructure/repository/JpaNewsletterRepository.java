package com.notify.newsletter.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNewsletterRepository extends JpaRepository<JpaNewsletterEntity, UUID> {

    Optional<JpaNewsletterEntity> findBySlug(String slug);

    List<JpaNewsletterEntity> findBySenderId(Long senderId);
}
