package com.notify.newsletter.infrastructure.repository;

import com.notify.newsletter.domain.model.Campaign;
import com.notify.newsletter.domain.model.CampaignStatus;
import org.springframework.stereotype.Component;

@Component
public class CampaignDomainMapper {

    public JpaCampaignEntity toJpa(Campaign domain) {
        if (domain == null) {
            return null;
        }

        return JpaCampaignEntity.builder().id(domain.getId()).newsletterId(domain.getNewsletterId())
                .subject(domain.getSubject()).content(domain.getContent()).status(domain.getStatus().name())
                .scheduledAt(domain.getScheduledAt()).createdAt(domain.getCreatedAt()).updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Campaign toDomain(JpaCampaignEntity entity) {
        if (entity == null) {
            return null;
        }

        return Campaign.builder().id(entity.getId()).newsletterId(entity.getNewsletterId()).subject(entity.getSubject())
                .content(entity.getContent() != null ? entity.getContent() : "")
                .status(CampaignStatus.valueOf(entity.getStatus())).scheduledAt(entity.getScheduledAt())
                .createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }
}
