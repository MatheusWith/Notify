package com.notify.newsletter.domain.repository;

import com.notify.newsletter.domain.model.Campaign;
import com.notify.newsletter.domain.model.CampaignStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CampaignRepository {

    Optional<Campaign> findById(UUID id);

    Page<Campaign> findByNewsletterId(UUID newsletterId, Pageable pageable);

    Page<Campaign> findByNewsletterIdAndStatus(UUID newsletterId, CampaignStatus status, Pageable pageable);

    Page<Campaign> findByNewsletterIdWithFilters(UUID newsletterId, String search, CampaignStatus status,
            Pageable pageable);

    Campaign save(Campaign campaign);

    void deleteById(UUID id);

    long countByNewsletterId(UUID newsletterId);
}
