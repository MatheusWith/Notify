package com.notify.identity.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

class UserControllerTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;
    private String adminToken;
    private String userToken;
    private Long newUserId;

    @BeforeEach
    void setUp() {
        var factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout(3000);
        factory.setReadTimeout(3000);
        restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatusCode statusCode) {
                return false;
            }
        });

        var loginRequest = Map.of("email", "admin@notify.com", "password", "Admin@123");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(authBaseUrl() + "/login", loginRequest,
                Map.class);
        adminToken = "Bearer " + loginResponse.getBody().get("accessToken");

        var registerRequest = Map.of("email", "testuser-" + System.currentTimeMillis() + "@test.com", "name",
                "Test User", "password", "UserP@ss1");
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(authBaseUrl() + "/register", registerRequest,
                Map.class);
        userToken = "Bearer " + registerResponse.getBody().get("accessToken");

        var headers = new HttpHeaders();
        headers.set("Authorization", userToken);
        var userEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> meResponse = restTemplate.exchange(usersBaseUrl() + "/me", HttpMethod.GET, userEntity,
                Map.class);
        newUserId = ((Number) meResponse.getBody().get("id")).longValue();
    }

    private String authBaseUrl() {
        return "http://localhost:" + port + "/api/v1/auth";
    }

    private String usersBaseUrl() {
        return "http://localhost:" + port + "/api/v1/users";
    }

    @Test
    void givenAuthenticated_whenGetMe_thenReturnsUser() {
        var headers = new HttpHeaders();
        headers.set("Authorization", adminToken);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(usersBaseUrl() + "/me", HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().containsKey("id").containsKey("email").containsKey("name")
                .containsKey("roles");
        assertThat(response.getBody().get("email")).isEqualTo("admin@notify.com");
    }

    @Test
    void givenNoToken_whenGetMe_thenReturns401() {
        ResponseEntity<Map> response = restTemplate.exchange(usersBaseUrl() + "/me", HttpMethod.GET, null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenAuthenticated_whenUpdateProfile_thenReturnsUpdated() {
        var headers = new HttpHeaders();
        headers.set("Authorization", userToken);
        var updateRequest = Map.of("name", "Updated Name", "email", "testuser@test.com");
        var entity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(usersBaseUrl() + "/me", HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().extracting("name").isEqualTo("Updated Name");
    }

    @Test
    void givenAdmin_whenGetAllUsers_thenReturnsPage() {
        var headers = new HttpHeaders();
        headers.set("Authorization", adminToken);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(usersBaseUrl() + "?page=0&size=20", HttpMethod.GET, entity,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().containsKey("content");
    }

    @Test
    void givenRegularUser_whenGetAllUsers_thenReturns403() {
        var headers = new HttpHeaders();
        headers.set("Authorization", userToken);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(usersBaseUrl(), HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void givenAdmin_whenGetUserById_thenReturnsUser() {
        var headers = new HttpHeaders();
        headers.set("Authorization", adminToken);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(usersBaseUrl() + "/" + newUserId, HttpMethod.GET, entity,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().extracting("id").isEqualTo(newUserId.intValue());
    }

    @Test
    void givenAdmin_whenGetNonexistentUser_thenReturns404() {
        var headers = new HttpHeaders();
        headers.set("Authorization", adminToken);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(usersBaseUrl() + "/99999", HttpMethod.GET, entity,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void givenAdmin_whenToggleUserStatus_thenToggles() {
        var headers = new HttpHeaders();
        headers.set("Authorization", adminToken);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<Void> disableResponse = restTemplate
                .exchange(usersBaseUrl() + "/" + newUserId + "/toggle-status", HttpMethod.PATCH, entity, Void.class);
        assertThat(disableResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> getUserResponse = restTemplate.exchange(usersBaseUrl() + "/" + newUserId, HttpMethod.GET,
                entity, Map.class);
        assertThat(getUserResponse.getBody()).extracting("enabled").isEqualTo(false);
    }

    @Test
    void givenRegularUser_whenToggleUserStatus_thenReturns403() {
        var headers = new HttpHeaders();
        headers.set("Authorization", userToken);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(usersBaseUrl() + "/" + newUserId + "/toggle-status",
                HttpMethod.PATCH, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
