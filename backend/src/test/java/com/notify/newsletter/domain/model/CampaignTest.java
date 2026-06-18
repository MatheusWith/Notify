package com.notify.newsletter.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CampaignTest {

    private Campaign createCampaign(CampaignStatus status) {
        return Campaign.builder().id(UUID.randomUUID()).newsletterId(UUID.randomUUID()).subject("Test Subject")
                .content("Test content").status(status).build();
    }

    @Test
    void givenDefaultBuilder_whenBuild_thenStatusIsDRAFT() {
        Campaign campaign = Campaign.builder().id(UUID.randomUUID()).newsletterId(UUID.randomUUID()).subject("Test")
                .content("Content").build();

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.DRAFT);
    }

    @Test
    void givenDraftCampaign_whenSubmit_thenStatusIsPENDING() {
        Campaign campaign = createCampaign(CampaignStatus.DRAFT);

        campaign.submit();

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.PENDING);
    }

    @Test
    void givenPendingCampaign_whenPublish_thenStatusIsPUBLISHED() {
        Campaign campaign = createCampaign(CampaignStatus.PENDING);

        campaign.publish();

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.PUBLISHED);
    }

    @Test
    void givenDraftCampaign_whenPublish_thenStatusIsPUBLISHED() {
        Campaign campaign = createCampaign(CampaignStatus.DRAFT);

        campaign.publish();

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.PUBLISHED);
    }

    @Test
    void givenPublishedCampaign_whenPublish_thenThrowsIllegalStateException() {
        Campaign campaign = createCampaign(CampaignStatus.PUBLISHED);

        assertThatThrownBy(campaign::publish).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenSentCampaign_whenPublish_thenThrowsIllegalStateException() {
        Campaign campaign = createCampaign(CampaignStatus.SENT);

        assertThatThrownBy(campaign::publish).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenDraftCampaign_whenSubmit_thenUpdatedAtChanges() {
        Campaign campaign = createCampaign(CampaignStatus.DRAFT);
        LocalDateTime before = campaign.getUpdatedAt();

        campaign.submit();

        assertThat(campaign.getUpdatedAt()).isAfter(before);
    }

    @Test
    void givenNonDraftCampaign_whenSubmit_thenThrowsIllegalStateException() {
        Campaign pending = createCampaign(CampaignStatus.PENDING);
        Campaign published = createCampaign(CampaignStatus.PUBLISHED);
        Campaign sent = createCampaign(CampaignStatus.SENT);

        assertThatThrownBy(pending::submit).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(published::submit).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(sent::submit).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenCampaign_whenIsEditable_thenReturnsTrueForDraftAndPending() {
        Campaign draft = createCampaign(CampaignStatus.DRAFT);
        Campaign pending = createCampaign(CampaignStatus.PENDING);

        assertThat(draft.isEditable()).isTrue();
        assertThat(pending.isEditable()).isTrue();
    }

    @Test
    void givenCampaign_whenIsEditable_thenReturnsFalseForPublishedAndSent() {
        Campaign published = createCampaign(CampaignStatus.PUBLISHED);
        Campaign sent = createCampaign(CampaignStatus.SENT);

        assertThat(published.isEditable()).isFalse();
        assertThat(sent.isEditable()).isFalse();
    }

    @Test
    void givenCampaign_whenIsDeletable_thenReturnsTrueForDraftAndPending() {
        Campaign draft = createCampaign(CampaignStatus.DRAFT);
        Campaign pending = createCampaign(CampaignStatus.PENDING);

        assertThat(draft.isDeletable()).isTrue();
        assertThat(pending.isDeletable()).isTrue();
    }

    @Test
    void givenCampaign_whenIsDeletable_thenReturnsFalseForPublishedAndSent() {
        Campaign published = createCampaign(CampaignStatus.PUBLISHED);
        Campaign sent = createCampaign(CampaignStatus.SENT);

        assertThat(published.isDeletable()).isFalse();
        assertThat(sent.isDeletable()).isFalse();
    }

    @Test
    void givenDraftCampaign_whenUpdateContent_thenFieldsAreUpdated() {
        Campaign campaign = createCampaign(CampaignStatus.DRAFT);
        LocalDateTime before = campaign.getUpdatedAt();

        campaign.updateContent("New Subject", "New content", LocalDateTime.now().plusDays(7));

        assertThat(campaign.getSubject()).isEqualTo("New Subject");
        assertThat(campaign.getContent()).isEqualTo("New content");
        assertThat(campaign.getScheduledAt()).isNotNull();
        assertThat(campaign.getUpdatedAt()).isAfter(before);
    }

    @Test
    void givenPublishedCampaign_whenUpdateContent_thenThrowsIllegalStateException() {
        Campaign campaign = createCampaign(CampaignStatus.PUBLISHED);

        assertThatThrownBy(() -> campaign.updateContent("Subject", "Content", null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenSentCampaign_whenUpdateContent_thenThrowsIllegalStateException() {
        Campaign campaign = createCampaign(CampaignStatus.SENT);

        assertThatThrownBy(() -> campaign.updateContent("Subject", "Content", null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenPublishedCampaign_whenMarkSent_thenStatusIsSENT() {
        Campaign campaign = createCampaign(CampaignStatus.PUBLISHED);

        campaign.markSent();

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SENT);
    }

    @Test
    void givenPublishedCampaign_whenMarkSent_thenUpdatedAtChanges() {
        Campaign campaign = createCampaign(CampaignStatus.PUBLISHED);
        LocalDateTime before = campaign.getUpdatedAt();

        campaign.markSent();

        assertThat(campaign.getUpdatedAt()).isAfter(before);
    }

    @Test
    void givenDraftCampaign_whenMarkSent_thenThrowsIllegalStateException() {
        Campaign campaign = createCampaign(CampaignStatus.DRAFT);

        assertThatThrownBy(campaign::markSent).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenPendingCampaign_whenMarkSent_thenThrowsIllegalStateException() {
        Campaign campaign = createCampaign(CampaignStatus.PENDING);

        assertThatThrownBy(campaign::markSent).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenSentCampaign_whenMarkSent_thenThrowsIllegalStateException() {
        Campaign campaign = createCampaign(CampaignStatus.SENT);

        assertThatThrownBy(campaign::markSent).isInstanceOf(IllegalStateException.class);
    }
}
