package com.notify.newsletter.infrastructure.repository;

import com.notify.newsletter.domain.model.Newsletter;
import com.notify.newsletter.domain.repository.NewsletterRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NewsletterRepositoryImpl implements NewsletterRepository {

    private final JpaNewsletterRepository jpaRepository;
    private final NewsletterDomainMapper mapper;

    @Override
    public Optional<Newsletter> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Newsletter> findBySlug(String slug) {
        return jpaRepository.findBySlug(slug).map(mapper::toDomain);
    }

    @Override
    public List<Newsletter> findBySenderId(Long senderId) {
        return jpaRepository.findBySenderId(senderId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Newsletter save(Newsletter newsletter) {
        JpaNewsletterEntity entity = mapper.toJpa(newsletter);
        entity = jpaRepository.save(entity);
        return mapper.toDomain(entity);
    }
}
