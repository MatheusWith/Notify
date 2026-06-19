package com.notify.newsletter.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.notify.newsletter.domain.model.*;
import com.notify.newsletter.domain.repository.NewsletterRepository;
import com.notify.newsletter.domain.repository.SubscriptionRepository;
import com.notify.shared.infrastructure.ratelimit.RateLimitFilter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

class NewsletterControllerTest extends AbstractNewsletterIntegrationTest {

    private static final UUID NEWSLETTER_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String AUTH_BASE = "/api/v1/auth";

    @LocalServerPort
    private int port;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private NewsletterRepository newsletterRepository;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        rateLimitFilter.reset();
        var httpClient = HttpClientBuilder.create().disableAutomaticRetries().build();
        var factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectionRequestTimeout(3000);
        factory.setReadTimeout(3000);
        restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatusCode statusCode) {
                return false;
            }
        });
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/newsletter";
    }

    private String authUrl() {
        return "http://localhost:" + port + AUTH_BASE;
    }

    private String loginAs(String email, String password) {
        var loginRequest = Map.of("email", email, "password", password);
        ResponseEntity<Map> response = restTemplate.postForEntity(authUrl() + "/login", loginRequest, Map.class);
        return "Bearer " + response.getBody().get("accessToken");
    }

    private Subscription createTestSubscription(UUID newsletterId, String email, Long subscriberId) {
        return subscriptionRepository.save(Subscription.builder().id(UUID.randomUUID())
                .email(new SubscriberEmail(email)).newsletterId(newsletterId).subscriberId(subscriberId)
                .token(UUID.randomUUID()).expiresAt(LocalDateTime.now().plusHours(24))
                .status(SubscriptionStatus.CONFIRMED).createdAt(LocalDateTime.now()).build());
    }

    private Newsletter createTestNewsletter(Long senderId, String name, String slug) {
        return newsletterRepository.save(
                Newsletter.builder().id(UUID.randomUUID()).senderId(senderId).name(name).slug(new Slug(slug)).build());
    }

    @Test
    void givenExistingSlug_whenGetNewsletterInfo_thenReturns200AndProfile() {
        ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl() + "/admin-announcements", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().containsEntry("name", "Admin Announcements")
                .containsEntry("slug", "admin-announcements").containsKey("id").containsKey("subscriberCount");
    }

    @Test
    void givenInvalidSlug_whenGetNewsletterInfo_thenReturns404() {
        ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl() + "/nonexistent-slug", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void givenValidEmail_whenSubscribe_thenReturns201AndPendingStatus() {
        var request = Map.of("email", "user@example.com");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/subscribe", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull().containsKey("id").containsEntry("email", "user@example.com")
                .containsEntry("status", "PENDING").containsKey("expiresAt");
    }

    @Test
    void givenInvalidEmail_whenSubscribe_thenReturns400() {
        var request = Map.of("email", "not-an-email");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/subscribe", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void givenExistingPendingEmail_whenSubscribeAgain_thenReturns201WithSameId() {
        var request = Map.of("email", "repeat@example.com");

        ResponseEntity<Map> first = restTemplate.postForEntity(baseUrl() + "/subscribe", request, Map.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String firstId = (String) first.getBody().get("id");

        ResponseEntity<Map> second = restTemplate.postForEntity(baseUrl() + "/subscribe", request, Map.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String secondId = (String) second.getBody().get("id");

        assertThat(secondId).isEqualTo(firstId);
    }

    @Test
    void givenInvalidToken_whenConfirm_thenReturns404() {
        var request = Map.of("token", UUID.randomUUID().toString());

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/confirm", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void givenMissingToken_whenConfirm_thenReturns400() {
        var request = Map.of("token", "");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/confirm", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void givenExistingEmail_whenResend_thenReturns200() {
        var subscribeReq = Map.of("email", "resend-me@example.com");
        restTemplate.postForEntity(baseUrl() + "/subscribe", subscribeReq, Map.class);

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/resend", subscribeReq, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().containsEntry("email", "resend-me@example.com");
    }

    @Test
    void givenNonExistentEmail_whenResend_thenReturns404() {
        var request = Map.of("email", "never-subscribed@example.com");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/resend", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void givenAuthenticatedOwner_whenListSubscribers_thenReturns200WithSubscribers() {
        Newsletter newsletter = createTestNewsletter(1L, "Test NL", "test-nl");
        createTestSubscription(newsletter.getId(), "sub1@example.com", 1L);
        createTestSubscription(newsletter.getId(), "sub2@example.com", 1L);

        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/test-nl/subscribers", HttpMethod.GET, entity,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("content")).isNotNull();

        var content = (java.util.List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(2);
        assertThat(content.get(0)).containsKey("name").containsKey("status").containsKey("createdAt");
        assertThat(content.get(0)).doesNotContainKey("email");
    }

    @Test
    void givenUnauthenticated_whenListSubscribers_thenReturns401() {
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/admin-announcements/subscribers",
                HttpMethod.GET, null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenNonOwner_whenListSubscribers_thenReturns403() {
        Newsletter newsletter = createTestNewsletter(1L, "Owner NL", "owner-nl");

        var registerRequest = Map.of("email", "other@example.com", "name", "Other User", "password", "StrongP@ss1");
        restTemplate.postForEntity(authUrl() + "/register", registerRequest, Map.class);

        String token = loginAs("other@example.com", "StrongP@ss1");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/owner-nl/subscribers", HttpMethod.GET,
                entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void givenAuthenticatedUserWithNewsletters_whenGetMyNewsletters_thenReturns200AndList() {
        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(baseUrl() + "/my", HttpMethod.GET, entity, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();
        var first = (java.util.Map<String, Object>) response.getBody().get(0);
        assertThat(first).containsKeys("id", "name", "slug", "subscriberCount");
    }

    @Test
    void givenUnauthenticated_whenGetMyNewsletters_thenReturns401() {
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/my", HttpMethod.GET, null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenInvalidSlug_whenListSubscribers_thenReturns404() {
        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/nonexistent/subscribers", HttpMethod.GET,
                entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void givenNewsletterWithNoSubscribers_whenListSubscribers_thenReturns200WithEmptyList() {
        Newsletter newsletter = createTestNewsletter(1L, "Empty NL", "empty-nl");

        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/empty-nl/subscribers", HttpMethod.GET,
                entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        var content = (java.util.List<?>) response.getBody().get("content");
        assertThat(content).isEmpty();
    }
}
