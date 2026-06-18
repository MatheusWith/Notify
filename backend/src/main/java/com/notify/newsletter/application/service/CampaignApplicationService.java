package com.notify.newsletter.application.service;

import com.notify.newsletter.application.dto.*;
import com.notify.newsletter.application.port.in.CampaignUseCase;
import com.notify.newsletter.domain.model.Campaign;
import com.notify.newsletter.domain.model.Newsletter;
import com.notify.newsletter.domain.repository.CampaignRepository;
import com.notify.newsletter.domain.repository.NewsletterRepository;
import com.notify.shared.application.BusinessException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class CampaignApplicationService implements CampaignUseCase {

    private final CampaignRepository campaignRepository;
    private final NewsletterRepository newsletterRepository;

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
                .orElseThrow(() -> new BusinessException(404, "Campaign not found"));

        return toResponse(campaign);
    }

    @Override
    public Page<CampaignResponse> list(String slug, Long senderId, Pageable pageable) {
        Newsletter newsletter = findNewsletterBySlug(slug);
        verifyOwnership(newsletter, senderId);

        return campaignRepository.findByNewsletterId(newsletter.getId(), pageable).map(this::toResponse);
    }

    @Override
    public CampaignResponse update(String slug, UUID campaignId, Long senderId, UpdateCampaignRequest request) {
        Newsletter newsletter = findNewsletterBySlug(slug);
        verifyOwnership(newsletter, senderId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new BusinessException(404, "Campaign not found"));

        try {
            campaign.updateContent(request.subject(), request.content(), request.scheduledAt());
        } catch (IllegalStateException e) {
            throw new BusinessException(409, e.getMessage());
        }

        campaign = campaignRepository.save(campaign);
        return toResponse(campaign);
    }

    @Override
    public void delete(String slug, UUID campaignId, Long senderId) {
        Newsletter newsletter = findNewsletterBySlug(slug);
        verifyOwnership(newsletter, senderId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new BusinessException(404, "Campaign not found"));

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
                .orElseThrow(() -> new BusinessException(404, "Campaign not found"));

        try {
            switch (request.status()) {
                case PENDING -> campaign.submit();
                case PUBLISHED -> campaign.publish();
                default -> throw new BusinessException(400, "Cannot transition to status: " + request.status());
            }
        } catch (IllegalStateException e) {
            throw new BusinessException(400, e.getMessage());
        }

        campaign = campaignRepository.save(campaign);
        return toResponse(campaign);
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
