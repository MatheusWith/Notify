package com.notify.newsletter.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notify.newsletter.application.dto.*;
import com.notify.newsletter.domain.model.Campaign;
import com.notify.newsletter.domain.model.CampaignStatus;
import com.notify.newsletter.domain.model.Newsletter;
import com.notify.newsletter.domain.model.Slug;
import com.notify.newsletter.domain.model.SubscriberEmail;
import com.notify.newsletter.domain.model.Subscription;
import com.notify.newsletter.domain.model.SubscriptionStatus;
import com.notify.newsletter.domain.repository.CampaignRepository;
import com.notify.newsletter.domain.repository.NewsletterRepository;
import com.notify.newsletter.domain.repository.SubscriptionRepository;
import com.notify.newsletter.infrastructure.messaging.NewsletterEventPublisher;
import com.notify.shared.application.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CampaignApplicationServiceTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private NewsletterRepository newsletterRepository;
    @Mock
    private NewsletterEventPublisher eventPublisher;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    private CampaignApplicationService service;

    private final Long senderId = 1L;
    private final UUID newsletterId = UUID.randomUUID();
    private final String slug = "test-newsletter";
    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        service = new CampaignApplicationService(campaignRepository, newsletterRepository, eventPublisher,
                subscriptionRepository);
    }

    private Newsletter createNewsletter(Long ownerId) {
        return Newsletter.builder().id(newsletterId).senderId(ownerId).name("Test Newsletter").slug(new Slug(slug))
                .build();
    }

    private Campaign createCampaign(UUID id, CampaignStatus status) {
        return Campaign.builder().id(id).newsletterId(newsletterId).subject("Test Subject").content("Test content")
                .status(status).build();
    }

    @Test
    void givenValidRequest_whenCreate_thenReturnsCreatedCampaign() {
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));
        when(campaignRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCampaignRequest request = new CreateCampaignRequest("Subject", "Content", null);
        CampaignResponse response = service.create(slug, senderId, request);

        assertThat(response.subject()).isEqualTo("Subject");
        assertThat(response.content()).isEqualTo("Content");
        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.scheduledAt()).isNull();
    }

    @Test
    void givenNonExistentSlug_whenCreate_thenThrows404() {
        when(newsletterRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        CreateCampaignRequest request = new CreateCampaignRequest("Subject", "Content", null);

        assertThatThrownBy(() -> service.create("nonexistent", senderId, request)).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 404);
    }

    @Test
    void givenNonOwner_whenCreate_thenThrows403() {
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));

        CreateCampaignRequest request = new CreateCampaignRequest("Subject", "Content", null);

        assertThatThrownBy(() -> service.create(slug, 99L, request)).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 403);
    }

    @Test
    void givenExistingCampaign_whenGetById_thenReturnsCampaign() {
        UUID campaignId = UUID.randomUUID();
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));
        when(campaignRepository.findById(campaignId))
                .thenReturn(Optional.of(createCampaign(campaignId, CampaignStatus.DRAFT)));

        CampaignResponse response = service.getById(slug, campaignId, senderId);

        assertThat(response.id()).isEqualTo(campaignId);
        assertThat(response.subject()).isEqualTo("Test Subject");
    }

    @Test
    void givenNonExistentCampaign_whenGetById_thenThrows404() {
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));
        when(campaignRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(slug, UUID.randomUUID(), senderId))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("status", 404);
    }

    @Test
    void givenMultipleCampaigns_whenList_thenReturnsPaginatedResults() {
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));
        when(campaignRepository.findByNewsletterId(newsletterId, pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(createCampaign(UUID.randomUUID(), CampaignStatus.DRAFT),
                        createCampaign(UUID.randomUUID(), CampaignStatus.PUBLISHED))));

        Page<CampaignResponse> result = service.list(slug, senderId, null, null, pageable);

        assertThat(result).hasSize(2);
    }

    @Test
    void givenNonOwner_whenList_thenThrows403() {
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));

        assertThatThrownBy(() -> service.list(slug, 99L, null, null, pageable)).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 403);
    }

    @Test
    void givenDraftCampaign_whenUpdate_thenReturnsUpdatedCampaign() {
        UUID campaignId = UUID.randomUUID();
        Campaign draft = createCampaign(campaignId, CampaignStatus.DRAFT);
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(draft));
        when(campaignRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCampaignRequest request = new UpdateCampaignRequest("Updated Subject", "Updated content", null);
        CampaignResponse response = service.update(slug, campaignId, senderId, request);

        assertThat(response.subject()).isEqualTo("Updated Subject");
        assertThat(response.content()).isEqualTo("Updated content");
    }

    @Test
    void givenPublishedCampaign_whenUpdate_thenThrows409() {
        UUID campaignId = UUID.randomUUID();
        Campaign published = createCampaign(campaignId, CampaignStatus.PUBLISHED);
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(published));

        UpdateCampaignRequest request = new UpdateCampaignRequest("Subject", "Content", null);

        assertThatThrownBy(() -> service.update(slug, campaignId, senderId, request))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("status", 409);
    }

    @Test
    void givenDraftCampaign_whenDelete_thenDeletes() {
        UUID campaignId = UUID.randomUUID();
        Campaign draft = createCampaign(campaignId, CampaignStatus.DRAFT);
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(draft));

        service.delete(slug, campaignId, senderId);

        verify(campaignRepository).deleteById(campaignId);
    }

    @Test
    void givenPublishedCampaign_whenDelete_thenThrows409() {
        UUID campaignId = UUID.randomUUID();
        Campaign published = createCampaign(campaignId, CampaignStatus.PUBLISHED);
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> service.delete(slug, campaignId, senderId)).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 409);
    }

    @Test
    void givenDraftCampaign_whenUpdateStatusToPENDING_thenReturnsPending() {
        UUID campaignId = UUID.randomUUID();
        Campaign draft = createCampaign(campaignId, CampaignStatus.DRAFT);
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(draft));
        when(campaignRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CampaignStatusRequest request = new CampaignStatusRequest(CampaignStatus.PENDING);
        CampaignResponse response = service.updateStatus(slug, campaignId, senderId, request);

        assertThat(response.status()).isEqualTo("PENDING");
    }

    @Test
    void givenDraftCampaign_whenUpdateStatusToPUBLISHED_thenReturnsPublished() {
        UUID campaignId = UUID.randomUUID();
        Campaign draft = createCampaign(campaignId, CampaignStatus.DRAFT);
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(draft));
        when(campaignRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.findByNewsletterId(any(), any())).thenReturn(new PageImpl<>(List.of()));

        CampaignStatusRequest request = new CampaignStatusRequest(CampaignStatus.PUBLISHED);
        CampaignResponse response = service.updateStatus(slug, campaignId, senderId, request);

        assertThat(response.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void givenPublishedTransition_whenUpdateStatus_thenPublishesMessage() {
        UUID campaignId = UUID.randomUUID();
        Campaign draft = createCampaign(campaignId, CampaignStatus.DRAFT);
        Newsletter newsletter = createNewsletter(senderId);
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(newsletter));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(draft));
        when(campaignRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription sub1 = Subscription.builder().id(UUID.randomUUID()).email(new SubscriberEmail("sub1@example.com"))
                .status(SubscriptionStatus.CONFIRMED).newsletterId(newsletterId).build();
        Subscription sub2 = Subscription.builder().id(UUID.randomUUID()).email(new SubscriberEmail("sub2@example.com"))
                .status(SubscriptionStatus.CONFIRMED).newsletterId(newsletterId).build();
        when(subscriptionRepository.findByNewsletterId(any(), any())).thenReturn(new PageImpl<>(List.of(sub1, sub2)));

        CampaignStatusRequest request = new CampaignStatusRequest(CampaignStatus.PUBLISHED);
        service.updateStatus(slug, campaignId, senderId, request);

        verify(eventPublisher).publishCampaignPublished(any());
    }

    @Test
    void givenPendingTransition_whenUpdateStatus_thenDoesNotPublish() {
        UUID campaignId = UUID.randomUUID();
        Campaign draft = createCampaign(campaignId, CampaignStatus.DRAFT);
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(draft));
        when(campaignRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CampaignStatusRequest request = new CampaignStatusRequest(CampaignStatus.PENDING);
        service.updateStatus(slug, campaignId, senderId, request);

        verify(eventPublisher, never()).publishCampaignPublished(any());
    }

    @Test
    void givenInvalidStatus_whenUpdateStatus_thenThrows400() {
        UUID campaignId = UUID.randomUUID();
        Campaign draft = createCampaign(campaignId, CampaignStatus.DRAFT);
        when(newsletterRepository.findBySlug(slug)).thenReturn(Optional.of(createNewsletter(senderId)));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(draft));

        CampaignStatusRequest request = new CampaignStatusRequest(CampaignStatus.SENT);

        assertThatThrownBy(() -> service.updateStatus(slug, campaignId, senderId, request))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("status", 400);
    }
}
