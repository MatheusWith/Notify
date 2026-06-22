package com.notify.worker.email.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.notify.worker.shared.domain.BusinessException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CampaignPublishedMessageTest {

    @Test
    void givenValidFields_whenCreated_thenSucceeds() {
        var message = new CampaignPublishedMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "my-slug",
                "Subject", "<html>content</html>", List.of("user1@example.com", "user2@example.com"));

        assertThat(message.messageId()).isNotNull();
        assertThat(message.campaignId()).isNotNull();
        assertThat(message.newsletterSlug()).isEqualTo("my-slug");
        assertThat(message.subscriberEmails()).hasSize(2);
    }

    @Test
    void givenNullMessageId_whenCreated_thenThrows() {
        assertThatThrownBy(() -> new CampaignPublishedMessage(null, UUID.randomUUID(), UUID.randomUUID(), "my-slug",
                "Subject", "content", List.of("user@example.com"))).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 400);
    }

    @Test
    void givenNullCampaignId_whenCreated_thenThrows() {
        assertThatThrownBy(() -> new CampaignPublishedMessage(UUID.randomUUID(), null, UUID.randomUUID(), "my-slug",
                "Subject", "content", List.of("user@example.com"))).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 400);
    }

    @Test
    void givenNullNewsletterSlug_whenCreated_thenThrows() {
        assertThatThrownBy(() -> new CampaignPublishedMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                null, "Subject", "content", List.of("user@example.com"))).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 400);
    }

    @Test
    void givenBlankSubject_whenCreated_thenThrows() {
        assertThatThrownBy(() -> new CampaignPublishedMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "my-slug", "  ", "content", List.of("user@example.com"))).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 400);
    }

    @Test
    void givenNullContent_whenCreated_thenThrows() {
        assertThatThrownBy(() -> new CampaignPublishedMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "my-slug", "Subject", null, List.of("user@example.com"))).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 400);
    }

    @Test
    void givenEmptyEmailList_whenCreated_thenThrows() {
        assertThatThrownBy(() -> new CampaignPublishedMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "my-slug", "Subject", "content", List.of())).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 400);
    }

    @Test
    void givenInvalidEmail_whenCreated_thenThrows() {
        assertThatThrownBy(() -> new CampaignPublishedMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "my-slug", "Subject", "content", List.of("not-an-email"))).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 400);
    }
}
