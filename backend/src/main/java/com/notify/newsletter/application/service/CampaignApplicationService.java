package com.notify.newsletter.application.service;

import com.notify.newsletter.application.dto.*;
import com.notify.newsletter.application.port.in.CampaignUseCase;
import com.notify.newsletter.domain.model.Campaign;
import com.notify.newsletter.domain.model.CampaignStatus;
import com.notify.newsletter.domain.model.Newsletter;
import com.notify.newsletter.domain.model.SubscriptionStatus;
import com.notify.newsletter.domain.repository.CampaignRepository;
import com.notify.newsletter.domain.repository.NewsletterRepository;
import com.notify.newsletter.domain.repository.SubscriptionRepository;
import com.notify.newsletter.infrastructure.messaging.CampaignPublishedMessage;
import com.notify.newsletter.infrastructure.messaging.NewsletterEventPublisher;
import com.notify.shared.application.BusinessException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CampaignApplicationService implements CampaignUseCase {

    private final CampaignRepository campaignRepository;
    private final NewsletterRepository newsletterRepository;
    private final NewsletterEventPublisher eventPublisher;
    private final SubscriptionRepository subscriptionRepository;

    private static final String CAMPAIGN_NOT_FOUND = "Campaign not found";

    @Override
    public CampaignResponse create(String slug, Long senderId, CreateCampaignRequest request) {
        Newsletter newsletter = findNewsletterBySlug(slug);
        verifyOwnership(newsletter, senderId);

        Campaign campaign = Campaign.builder().id(UUID.randomUUID()).newsletterId(newsletter.getId())
                .subject(request.subject()).content(request.content()).scheduledAt(request.scheduledAt()).build();

        campaign = campaignRepository.save(campaign);
        return toResponse(campaign);
    }

    @Override
    public CampaignResponse getById(String slug, UUID campaignId, Long senderId) {
        Newsletter newsletter = findNewsletterBySlug(slug);
        verifyOwnership(newsletter, senderId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new BusinessException(404, CAMPAIGN_NOT_FOUND));

        return toResponse(campaign);
    }

    @Override
    public Page<CampaignResponse> list(String slug, Long senderId, String search, CampaignStatus status,
            Pageable pageable) {
        Newsletter newsletter = findNewsletterBySlug(slug);
        verifyOwnership(newsletter, senderId);

        UUID newsletterId = newsletter.getId();
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasStatus = status != null;

        if (hasSearch) {
            return campaignRepository
                    .findByNewsletterIdWithFilters(newsletterId, search, hasStatus ? status : null, pageable)
                    .map(this::toResponse);
        }

        if (hasStatus) {
            return campaignRepository.findByNewsletterIdAndStatus(newsletterId, status, pageable).map(this::toResponse);
        }

        return campaignRepository.findByNewsletterId(newsletterId, pageable).map(this::toResponse);
    }

    @Override
    public CampaignResponse update(String slug, UUID campaignId, Long senderId, UpdateCampaignRequest request) {
        Newsletter newsletter = findNewsletterBySlug(slug);
        verifyOwnership(newsletter, senderId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new BusinessException(404, CAMPAIGN_NOT_FOUND));

        try {
            campaign.updateContent(request.subject(), request.content(), request.scheduledAt());
        } catch (IllegalStateException e) {
            throw new BusinessException(409, e.getMessage(), e);
        }

        campaign = campaignRepository.save(campaign);
        return toResponse(campaign);
    }

    @Override
    public void delete(String slug, UUID campaignId, Long senderId) {
        Newsletter newsletter = findNewsletterBySlug(slug);
        verifyOwnership(newsletter, senderId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new BusinessException(404, CAMPAIGN_NOT_FOUND));

        if (!campaign.isDeletable()) {
            throw new BusinessException(409, "Cannot delete a campaign with status " + campaign.getStatus());
        }

        campaignRepository.deleteById(campaignId);
    }

    @Override
    public CampaignResponse updateStatus(String slug, UUID campaignId, Long senderId, CampaignStatusRequest request) {
        Newsletter newsletter = findNewsletterBySlug(slug);
        verifyOwnership(newsletter, senderId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new BusinessException(404, CAMPAIGN_NOT_FOUND));

        try {
            switch (request.status()) {
                case PENDING -> campaign.submit();
                case PUBLISHED -> campaign.publish();
                default -> throw new BusinessException(400, "Cannot transition to status: " + request.status());
            }
        } catch (IllegalStateException e) {
            throw new BusinessException(400, e.getMessage(), e);
        }

        campaign = campaignRepository.save(campaign);

        if (request.status() == CampaignStatus.PUBLISHED) {
            publishCampaign(campaign, newsletter);
        }

        return toResponse(campaign);
    }

    private void publishCampaign(Campaign campaign, Newsletter newsletter) {
        try {
            List<String> emails = subscriptionRepository.findByNewsletterId(newsletter.getId(), Pageable.unpaged())
                    .stream().filter(sub -> sub.getStatus() == SubscriptionStatus.CONFIRMED)
                    .map(sub -> sub.getEmail().value()).toList();

            if (emails.isEmpty()) {
                if (log.isWarnEnabled()) {
                    log.warn("No confirmed subscribers for newsletter {}, skipping campaign publication",
                            newsletter.getSlug().value());
                }
                return;
            }

            CampaignPublishedMessage message = new CampaignPublishedMessage(UUID.randomUUID(), campaign.getId(),
                    campaign.getNewsletterId(), newsletter.getSlug().value(), campaign.getSubject(),
                    campaign.getContent(), emails);

            eventPublisher.publishCampaignPublished(message);

            if (log.isInfoEnabled()) {
                log.info("Published campaign {} to {} subscribers", campaign.getId(), emails.size());
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to publish campaign {}: {}", campaign.getId(), e.getMessage());
            }
        }
    }

    private Newsletter findNewsletterBySlug(String slug) {
        return newsletterRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(404, "Newsletter not found"));
    }

    private void verifyOwnership(Newsletter newsletter, Long senderId) {
        if (!newsletter.getSenderId().equals(senderId)) {
            throw new BusinessException(403, "You are not the owner of this newsletter");
        }
    }

    private CampaignResponse toResponse(Campaign campaign) {
        return new CampaignResponse(campaign.getId(), campaign.getNewsletterId(), campaign.getSubject(),
                campaign.getContent(), campaign.getStatus().name(), campaign.getScheduledAt(), campaign.getCreatedAt(),
                campaign.getUpdatedAt());
    }
}
