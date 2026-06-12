package com.notify.newsletter.application.service;

import com.notify.identity.domain.model.User;
import com.notify.identity.domain.model.UserId;
import com.notify.identity.domain.repository.UserRepository;
import com.notify.newsletter.application.dto.SubscriberResponse;
import com.notify.newsletter.domain.model.*;
import com.notify.newsletter.domain.repository.NewsletterRepository;
import com.notify.newsletter.domain.repository.SubscriptionRepository;
import com.notify.newsletter.infrastructure.config.NewsletterProperties;
import com.notify.newsletter.infrastructure.messaging.NewsletterEventPublisher;
import com.notify.shared.application.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsletterApplicationServiceTest {

    @Mock
    private NewsletterRepository newsletterRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private NewsletterEventPublisher eventPublisher;
    @Mock
    private NewsletterProperties properties;
    @Mock
    private UserRepository userRepository;

    private NewsletterApplicationService service;

    private final Long senderId = 1L;
    private final UUID newsletterId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        service = new NewsletterApplicationService(
                newsletterRepository, subscriptionRepository,
                eventPublisher, properties, userRepository
        );
    }

    private Newsletter createNewsletter(Long ownerId) {
        return Newsletter.builder()
                .id(newsletterId)
                .senderId(ownerId)
                .name("Test Newsletter")
                .slug(new Slug("test-newsletter"))
                .build();
    }

    private Subscription createSubscription(Long subscriberId) {
        return Subscription.builder()
                .id(UUID.randomUUID())
                .email(new SubscriberEmail("sub" + subscriberId + "@example.com"))
                .newsletterId(newsletterId)
                .subscriberId(subscriberId)
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .status(SubscriptionStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void givenValidSlugAndOwner_whenListSubscribers_thenReturnsSubscribersWithNames() {
        Newsletter newsletter = createNewsletter(senderId);
        Subscription sub1 = createSubscription(10L);
        Subscription sub2 = createSubscription(20L);

        when(newsletterRepository.findBySlug("test-newsletter")).thenReturn(Optional.of(newsletter));
        when(subscriptionRepository.findByNewsletterId(newsletterId, pageable))
                .thenReturn(new PageImpl<>(List.of(sub1, sub2)));
        when(userRepository.findById(UserId.of(10L))).thenReturn(Optional.of(createUser(10L, "Alice")));
        when(userRepository.findById(UserId.of(20L))).thenReturn(Optional.of(createUser(20L, "Bob")));

        Page<SubscriberResponse> result = service.listSubscribers("test-newsletter", senderId, pageable);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SubscriberResponse::name)
                .containsExactlyInAnyOrder("Alice", "Bob");
        assertThat(result).extracting(SubscriberResponse::status)
                .allMatch(s -> s.equals("CONFIRMED"));
        assertThat(result).extracting(SubscriberResponse::createdAt)
                .isNotNull();
    }

    @Test
    void givenNonExistentSlug_whenListSubscribers_thenThrows404() {
        when(newsletterRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listSubscribers("nonexistent", senderId, pageable))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 404);
    }

    @Test
    void givenNonOwnerSender_whenListSubscribers_thenThrows403() {
        Newsletter newsletter = createNewsletter(senderId);
        Long otherSender = 99L;

        when(newsletterRepository.findBySlug("test-newsletter")).thenReturn(Optional.of(newsletter));

        assertThatThrownBy(() -> service.listSubscribers("test-newsletter", otherSender, pageable))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", 403);
    }

    @Test
    void givenSubscribersWithoutUserId_whenListSubscribers_thenShowsEmailAsName() {
        Newsletter newsletter = createNewsletter(senderId);

        when(newsletterRepository.findBySlug("test-newsletter")).thenReturn(Optional.of(newsletter));

        Subscription withUserId = createSubscription(10L);
        Subscription withoutUserId = createSubscription(null);

        when(subscriptionRepository.findByNewsletterId(newsletterId, pageable))
                .thenReturn(new PageImpl<>(List.of(withUserId, withoutUserId)));
        when(userRepository.findById(UserId.of(10L))).thenReturn(Optional.of(createUser(10L, "Alice")));

        Page<SubscriberResponse> result = service.listSubscribers("test-newsletter", senderId, pageable);

        assertThat(result).hasSize(2);
        assertThat(result.getContent()).extracting(SubscriberResponse::name)
                .containsExactlyInAnyOrder("Alice", "subnull@example.com");
    }

    @Test
    void givenSubscriberUserNotFound_whenListSubscribers_thenReturnsUnknown() {
        Newsletter newsletter = createNewsletter(senderId);

        when(newsletterRepository.findBySlug("test-newsletter")).thenReturn(Optional.of(newsletter));

        Subscription sub = createSubscription(999L);

        when(subscriptionRepository.findByNewsletterId(newsletterId, pageable))
                .thenReturn(new PageImpl<>(List.of(sub)));
        when(userRepository.findById(UserId.of(999L))).thenReturn(Optional.empty());

        Page<SubscriberResponse> result = service.listSubscribers("test-newsletter", senderId, pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Unknown");
    }

    @Test
    void givenEmptyNewsletter_whenListSubscribers_thenReturnsEmptyPage() {
        Newsletter newsletter = createNewsletter(senderId);

        when(newsletterRepository.findBySlug("test-newsletter")).thenReturn(Optional.of(newsletter));
        when(subscriptionRepository.findByNewsletterId(newsletterId, pageable))
                .thenReturn(Page.empty());

        Page<SubscriberResponse> result = service.listSubscribers("test-newsletter", senderId, pageable);

        assertThat(result).isEmpty();
    }

    private User createUser(Long id, String name) {
        return User.builder()
                .id(UserId.of(id))
                .name(name)
                .build();
    }
}
