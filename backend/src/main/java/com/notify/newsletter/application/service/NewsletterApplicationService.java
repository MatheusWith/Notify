package com.notify.newsletter.application.service;

import com.notify.identity.domain.model.User;
import com.notify.identity.domain.model.UserId;
import com.notify.identity.domain.repository.UserRepository;
import com.notify.newsletter.application.dto.*;
import com.notify.newsletter.application.port.in.NewsletterUseCase;
import com.notify.newsletter.domain.model.*;
import com.notify.newsletter.domain.repository.NewsletterRepository;
import com.notify.newsletter.domain.repository.SubscriptionRepository;
import com.notify.newsletter.infrastructure.config.NewsletterProperties;
import com.notify.newsletter.infrastructure.messaging.NewsletterEventPublisher;
import com.notify.shared.application.BusinessException;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class NewsletterApplicationService implements NewsletterUseCase {

    private final NewsletterRepository newsletterRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NewsletterEventPublisher eventPublisher;
    private final NewsletterProperties properties;
    private final UserRepository userRepository;

    @Override
    public NewsletterProfileResponse getNewsletterInfo(String slug) {
        Newsletter newsletter = newsletterRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(404, "Newsletter not found"));

        long subscriberCount = subscriptionRepository.countByNewsletterIdAndStatus(newsletter.getId(),
                SubscriptionStatus.CONFIRMED);

        return new NewsletterProfileResponse(newsletter.getId(), newsletter.getName(), newsletter.getSlug().value(),
                newsletter.getDescription(), subscriberCount);
    }

    @Override
    public List<NewsletterSummaryResponse> getMyNewsletters(Long senderId) {
        return newsletterRepository.findBySenderId(senderId).stream().map(n -> {
            long subscriberCount = subscriptionRepository.countByNewsletterIdAndStatus(n.getId(),
                    SubscriptionStatus.CONFIRMED);
            return new NewsletterSummaryResponse(n.getId(), n.getName(), n.getSlug().value(), subscriberCount);
        }).toList();
    }

    @Override
    public SubscribeResponse subscribe(SubscribeRequest request) {
        SubscriberEmail email = new SubscriberEmail(request.email());

        UUID newsletterId = resolveNewsletterId(request.slug());

        var existingOpt = subscriptionRepository.findByEmailAndStatus(email, SubscriptionStatus.PENDING);

        if (existingOpt.isPresent()) {
            Subscription existing = existingOpt.get();
            UUID newToken = UUID.randomUUID();
            LocalDateTime newExpiresAt = LocalDateTime.now()
                    .plusHours(properties.getConfirmationTokenExpirationHours());
            existing.refreshToken(newToken, newExpiresAt);
            subscriptionRepository.save(existing);

            eventPublisher.publishConfirmationEmail(existing);

            return new SubscribeResponse(existing.getId(), existing.getEmail().value(), existing.getStatus().name(),
                    existing.getExpiresAt());
        }

        UUID id = UUID.randomUUID();
        UUID token = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(properties.getConfirmationTokenExpirationHours());

        Subscription subscription = Subscription.builder().id(id).email(email).newsletterId(newsletterId).token(token)
                .expiresAt(expiresAt).build();

        subscription = subscriptionRepository.save(subscription);

        eventPublisher.publishConfirmationEmail(subscription);

        return new SubscribeResponse(subscription.getId(), subscription.getEmail().value(),
                subscription.getStatus().name(), subscription.getExpiresAt());
    }

    private UUID resolveNewsletterId(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        Newsletter newsletter = newsletterRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(404, "Newsletter not found: " + slug));
        return newsletter.getId();
    }

    @Override
    public ConfirmationResponse confirm(ConfirmSubscriptionRequest request) {
        UUID token = request.token();
        Subscription subscription = subscriptionRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(404, "Invalid confirmation token"));

        if (subscription.isExpired()) {
            subscription.refreshToken(UUID.randomUUID(),
                    LocalDateTime.now().plusHours(properties.getConfirmationTokenExpirationHours()));
            subscriptionRepository.save(subscription);
            eventPublisher.publishConfirmationEmail(subscription);
            throw new BusinessException(410, "Confirmation link has expired. A new confirmation email has been sent.");
        }

        if (subscription.isConfirmed()) {
            return new ConfirmationResponse(subscription.getStatus().name(), subscription.getEmail().value());
        }

        subscription.confirm();
        subscriptionRepository.save(subscription);

        return new ConfirmationResponse(subscription.getStatus().name(), subscription.getEmail().value());
    }

    @Override
    public Page<SubscriberResponse> listSubscribers(String slug, Long senderId, Pageable pageable) {
        Newsletter newsletter = newsletterRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(404, "Newsletter not found"));

        if (!newsletter.getSenderId().equals(senderId)) {
            throw new BusinessException(403, "Not the owner of this newsletter");
        }

        Page<Subscription> subscriptions = subscriptionRepository.findByNewsletterId(newsletter.getId(), pageable);

        List<SubscriberResponse> responses = subscriptions.stream().map(sub -> {
            String name;
            if (sub.getSubscriberId() != null) {
                name = userRepository.findById(UserId.of(sub.getSubscriberId())).map(User::getName).orElse("Unknown");
            } else {
                name = sub.getEmail().value();
            }
            return new SubscriberResponse(name, sub.getStatus().name(), sub.getCreatedAt());
        }).toList();

        return new PageImpl<>(responses, pageable, subscriptions.getTotalElements());
    }

    @Override
    public SubscribeResponse resendConfirmation(SubscribeRequest request) {
        SubscriberEmail email = new SubscriberEmail(request.email());

        Subscription subscription = subscriptionRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(404, "No subscription found for this email"));

        UUID newToken = UUID.randomUUID();
        LocalDateTime newExpiresAt = LocalDateTime.now().plusHours(properties.getConfirmationTokenExpirationHours());
        subscription.refreshToken(newToken, newExpiresAt);
        subscriptionRepository.save(subscription);

        eventPublisher.publishConfirmationEmail(subscription);

        return new SubscribeResponse(subscription.getId(), subscription.getEmail().value(),
                subscription.getStatus().name(), subscription.getExpiresAt());
    }
}
