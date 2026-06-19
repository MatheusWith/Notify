package com.notify.newsletter.infrastructure.repository;

import com.notify.newsletter.domain.model.Newsletter;
import com.notify.newsletter.domain.model.Slug;
import org.springframework.stereotype.Component;

@Component
public class NewsletterDomainMapper {

    public JpaNewsletterEntity toJpa(Newsletter domain) {
        if (domain == null)
            return null;

        return JpaNewsletterEntity.builder().id(domain.getId()).senderId(domain.getSenderId()).name(domain.getName())
                .slug(domain.getSlug().value()).description(domain.getDescription()).createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt()).build();
    }

    public Newsletter toDomain(JpaNewsletterEntity entity) {
        if (entity == null)
            return null;

        return Newsletter.builder().id(entity.getId()).senderId(entity.getSenderId()).name(entity.getName())
                .slug(new Slug(entity.getSlug()))
                .description(entity.getDescription() != null ? entity.getDescription() : "")
                .createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }
}
