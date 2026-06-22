package com.notify.newsletter.infrastructure.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaCampaignRepository extends JpaRepository<JpaCampaignEntity, UUID> {

    Page<JpaCampaignEntity> findByNewsletterId(UUID newsletterId, Pageable pageable);

    Page<JpaCampaignEntity> findByNewsletterIdAndStatus(UUID newsletterId, String status, Pageable pageable);

    @Query("""
            SELECT c FROM JpaCampaignEntity c
            WHERE c.newsletterId = :newsletterId
            AND (:search IS NULL OR LOWER(c.subject) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:status IS NULL OR c.status = :status)
            """)
    Page<JpaCampaignEntity> findByNewsletterIdWithFilters(@Param("newsletterId") UUID newsletterId,
            @Param("search") String search, @Param("status") String status, Pageable pageable);

    long countByNewsletterId(UUID newsletterId);
}
