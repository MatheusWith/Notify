package com.notify.newsletter.infrastructure.repository;

import com.notify.newsletter.domain.model.SubscriberEmail;
import com.notify.newsletter.domain.model.Subscription;
import com.notify.newsletter.domain.model.SubscriptionStatus;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionDomainMapper {

    public JpaSubscriptionEntity toJpa(Subscription domain) {
        if (domain == null) return null;

        return JpaSubscriptionEntity.builder()
                .id(domain.getId())
                .email(domain.getEmail().value())
                .newsletterId(domain.getNewsletterId())
                .subscriberId(domain.getSubscriberId())
                .status(domain.getStatus().name())
                .token(domain.getToken())
                .expiresAt(domain.getExpiresAt())
                .confirmedAt(domain.getConfirmedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Subscription toDomain(JpaSubscriptionEntity entity) {
        if (entity == null) return null;

        return Subscription.builder()
                .id(entity.getId())
                .email(new SubscriberEmail(entity.getEmail()))
                .newsletterId(entity.getNewsletterId())
                .subscriberId(entity.getSubscriberId())
                .status(SubscriptionStatus.valueOf(entity.getStatus()))
                .token(entity.getToken())
                .expiresAt(entity.getExpiresAt())
                .confirmedAt(entity.getConfirmedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
