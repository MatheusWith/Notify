package com.notify.newsletter.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class NewsletterTest {

    @Test
    void givenValidData_whenBuilt_thenSucceeds() {
        Slug slug = new Slug("tech-weekly");
        Newsletter newsletter = Newsletter.builder().id(UUID.randomUUID()).senderId(1L).name("Tech Weekly").slug(slug)
                .description("Latest tech news").build();

        assertThat(newsletter.getId()).isNotNull();
        assertThat(newsletter.getSenderId()).isEqualTo(1L);
        assertThat(newsletter.getName()).isEqualTo("Tech Weekly");
        assertThat(newsletter.getSlug()).isEqualTo(slug);
        assertThat(newsletter.getDescription()).isEqualTo("Latest tech news");
        assertThat(newsletter.getCreatedAt()).isNotNull();
        assertThat(newsletter.getUpdatedAt()).isNotNull();
    }

    @Test
    void givenNoDescription_whenBuilt_thenDefaultsToEmpty() {
        Newsletter newsletter = Newsletter.builder().id(UUID.randomUUID()).senderId(1L).name("DevOps Digest")
                .slug(new Slug("devops-digest")).build();

        assertThat(newsletter.getDescription()).isEmpty();
    }

    @Test
    void givenValidSlug_thenNormalizesToLowercase() {
        Slug slug = new Slug("Tech-Weekly");
        assertThat(slug.value()).isEqualTo("tech-weekly");
    }

    @Test
    void givenInvalidSlug_thenThrowsException() {
        assertThatThrownBy(() -> new Slug("Invalid Slug!")).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Slug("")).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Slug("  ")).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Slug("-leading-hyphen")).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Slug("has spaces")).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Slug("special@chars")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNullSlug_thenThrowsException() {
        assertThatThrownBy(() -> new Slug(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
