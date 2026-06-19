package com.notify.newsletter.infrastructure.repository;

import com.notify.newsletter.domain.model.Campaign;
import com.notify.newsletter.domain.model.CampaignStatus;
import com.notify.newsletter.domain.repository.CampaignRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CampaignRepositoryImpl implements CampaignRepository {

    private final JpaCampaignRepository jpaRepository;
    private final CampaignDomainMapper mapper;

    @Override
    public Optional<Campaign> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Campaign> findByNewsletterId(UUID newsletterId, Pageable pageable) {
        return jpaRepository.findByNewsletterId(newsletterId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Campaign> findByNewsletterIdAndStatus(UUID newsletterId, CampaignStatus status, Pageable pageable) {
        return jpaRepository.findByNewsletterIdAndStatus(newsletterId, status.name(), pageable).map(mapper::toDomain);
    }

    @Override
    public Campaign save(Campaign campaign) {
        JpaCampaignEntity entity = mapper.toJpa(campaign);
        entity = jpaRepository.save(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public long countByNewsletterId(UUID newsletterId) {
        return jpaRepository.countByNewsletterId(newsletterId);
    }
}
