package com.notify.newsletter.infrastructure.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCampaignRepository extends JpaRepository<JpaCampaignEntity, UUID> {

    Page<JpaCampaignEntity> findByNewsletterId(UUID newsletterId, Pageable pageable);

    Page<JpaCampaignEntity> findByNewsletterIdAndStatus(UUID newsletterId, String status, Pageable pageable);

    long countByNewsletterId(UUID newsletterId);
}
