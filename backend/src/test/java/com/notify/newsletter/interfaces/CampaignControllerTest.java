package com.notify.newsletter.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.notify.newsletter.domain.model.*;
import com.notify.newsletter.domain.repository.NewsletterRepository;
import com.notify.shared.infrastructure.ratelimit.RateLimitFilter;
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

class CampaignControllerTest extends AbstractNewsletterIntegrationTest {

    private static final String AUTH_BASE = "/api/v1/auth";

    @LocalServerPort
    private int port;

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

    private Newsletter createTestNewsletter(Long senderId, String name, String slug) {
        return newsletterRepository.save(
                Newsletter.builder().id(UUID.randomUUID()).senderId(senderId).name(name).slug(new Slug(slug)).build());
    }

    @Test
    void givenValidRequest_whenCreateCampaign_thenReturns201AndDraftStatus() {
        Newsletter newsletter = createTestNewsletter(1L, "Test NL", "test-nl");
        String token = loginAs("admin@notify.com", "Admin@123");

        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<>(Map.of("subject", "Hello", "content", "World"), headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/test-nl/campaigns", HttpMethod.POST, request,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull().containsEntry("subject", "Hello").containsEntry("content", "World")
                .containsEntry("status", "DRAFT").containsKey("id").containsKey("newsletterId");
    }

    @Test
    void givenNonExistentSlug_whenCreateCampaign_thenReturns404() {
        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<>(Map.of("subject", "Hello", "content", "World"), headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/nonexistent/campaigns", HttpMethod.POST,
                request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void givenNonOwner_whenCreateCampaign_thenReturns403() {
        Newsletter newsletter = createTestNewsletter(1L, "Owner NL", "owner-nl");

        var registerRequest = Map.of("email", "other@example.com", "name", "Other User", "password", "StrongP@ss1");
        restTemplate.postForEntity(authUrl() + "/register", registerRequest, Map.class);

        String token = loginAs("other@example.com", "StrongP@ss1");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<>(Map.of("subject", "Hello", "content", "World"), headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/owner-nl/campaigns", HttpMethod.POST,
                request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void givenUnauthenticated_whenCreateCampaign_thenReturns401() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<>(Map.of("subject", "Hello", "content", "World"), headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/test-nl/campaigns", HttpMethod.POST, request,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenExistingCampaigns_whenListCampaigns_thenReturns200WithPagination() {
        Newsletter newsletter = createTestNewsletter(1L, "List NL", "list-nl");
        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var createRequest = new HttpEntity<>(Map.of("subject", "Campaign 1", "content", "Content 1"), headers);
        restTemplate.exchange(baseUrl() + "/list-nl/campaigns", HttpMethod.POST, createRequest, Map.class);
        restTemplate.exchange(baseUrl() + "/list-nl/campaigns", HttpMethod.POST, createRequest, Map.class);

        var entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/list-nl/campaigns", HttpMethod.GET, entity,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("content")).isNotNull();
        var content = (java.util.List<?>) response.getBody().get("content");
        assertThat(content).hasSize(2);
    }

    @Test
    void givenExistingCampaign_whenGetCampaign_thenReturns200() {
        Newsletter newsletter = createTestNewsletter(1L, "Get NL", "get-nl");
        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var createRequest = new HttpEntity<>(Map.of("subject", "Get Me", "content", "Content"), headers);
        ResponseEntity<Map> createResponse = restTemplate.exchange(baseUrl() + "/get-nl/campaigns", HttpMethod.POST,
                createRequest, Map.class);
        String campaignId = (String) createResponse.getBody().get("id");

        var entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/get-nl/campaigns/" + campaignId,
                HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().containsEntry("subject", "Get Me").containsEntry("status", "DRAFT");
    }

    @Test
    void givenNonExistentCampaign_whenGetCampaign_thenReturns404() {
        Newsletter newsletter = createTestNewsletter(1L, "Get404 NL", "get404-nl");
        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/get404-nl/campaigns/" + UUID.randomUUID(),
                HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void givenDraftCampaign_whenUpdate_thenReturns200WithUpdatedFields() {
        Newsletter newsletter = createTestNewsletter(1L, "Update NL", "update-nl");
        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var createRequest = new HttpEntity<>(Map.of("subject", "Before", "content", "Before content"), headers);
        ResponseEntity<Map> createResponse = restTemplate.exchange(baseUrl() + "/update-nl/campaigns", HttpMethod.POST,
                createRequest, Map.class);
        String campaignId = (String) createResponse.getBody().get("id");

        var updateRequest = new HttpEntity<>(Map.of("subject", "After", "content", "After content"), headers);
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/update-nl/campaigns/" + campaignId,
                HttpMethod.PUT, updateRequest, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().containsEntry("subject", "After").containsEntry("content",
                "After content");
    }

    @Test
    void givenDraftCampaign_whenPublish_thenReturns200AndPublishedStatus() {
        Newsletter newsletter = createTestNewsletter(1L, "Pub NL", "pub-nl");
        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var createRequest = new HttpEntity<>(Map.of("subject", "Pub", "content", "Content"), headers);
        ResponseEntity<Map> createResponse = restTemplate.exchange(baseUrl() + "/pub-nl/campaigns", HttpMethod.POST,
                createRequest, Map.class);
        String campaignId = (String) createResponse.getBody().get("id");

        var statusRequest = new HttpEntity<>(Map.of("status", "PUBLISHED"), headers);
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/pub-nl/campaigns/" + campaignId + "/status",
                HttpMethod.PATCH, statusRequest, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().containsEntry("status", "PUBLISHED");
    }

    @Test
    void givenPublishedCampaign_whenUpdate_thenReturns409() {
        Newsletter newsletter = createTestNewsletter(1L, "PubEdit NL", "pubedit-nl");
        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var createRequest = new HttpEntity<>(Map.of("subject", "Pub", "content", "Content"), headers);
        ResponseEntity<Map> createResponse = restTemplate.exchange(baseUrl() + "/pubedit-nl/campaigns", HttpMethod.POST,
                createRequest, Map.class);
        String campaignId = (String) createResponse.getBody().get("id");

        var statusRequest = new HttpEntity<>(Map.of("status", "PUBLISHED"), headers);
        restTemplate.exchange(baseUrl() + "/pubedit-nl/campaigns/" + campaignId + "/status", HttpMethod.PATCH,
                statusRequest, Map.class);

        var updateRequest = new HttpEntity<>(Map.of("subject", "Should fail", "content", "Content"), headers);
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/pubedit-nl/campaigns/" + campaignId,
                HttpMethod.PUT, updateRequest, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void givenDraftCampaign_whenDelete_thenReturns204() {
        Newsletter newsletter = createTestNewsletter(1L, "Del NL", "del-nl");
        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var createRequest = new HttpEntity<>(Map.of("subject", "Delete Me", "content", "Content"), headers);
        ResponseEntity<Map> createResponse = restTemplate.exchange(baseUrl() + "/del-nl/campaigns", HttpMethod.POST,
                createRequest, Map.class);
        String campaignId = (String) createResponse.getBody().get("id");

        var entity = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange(baseUrl() + "/del-nl/campaigns/" + campaignId,
                HttpMethod.DELETE, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void givenPublishedCampaign_whenDelete_thenReturns409() {
        Newsletter newsletter = createTestNewsletter(1L, "PubDel NL", "pubdel-nl");
        String token = loginAs("admin@notify.com", "Admin@123");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var createRequest = new HttpEntity<>(Map.of("subject", "Can't Delete", "content", "Content"), headers);
        ResponseEntity<Map> createResponse = restTemplate.exchange(baseUrl() + "/pubdel-nl/campaigns", HttpMethod.POST,
                createRequest, Map.class);
        String campaignId = (String) createResponse.getBody().get("id");

        var statusRequest = new HttpEntity<>(Map.of("status", "PUBLISHED"), headers);
        restTemplate.exchange(baseUrl() + "/pubdel-nl/campaigns/" + campaignId + "/status", HttpMethod.PATCH,
                statusRequest, Map.class);

        var entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/pubdel-nl/campaigns/" + campaignId,
                HttpMethod.DELETE, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
