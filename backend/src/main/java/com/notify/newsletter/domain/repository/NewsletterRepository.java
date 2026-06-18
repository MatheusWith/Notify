package com.notify.newsletter.domain.repository;

import com.notify.newsletter.domain.model.Newsletter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsletterRepository {

    Optional<Newsletter> findById(UUID id);

    Optional<Newsletter> findBySlug(String slug);

    List<Newsletter> findBySenderId(Long senderId);

    Newsletter save(Newsletter newsletter);
}
