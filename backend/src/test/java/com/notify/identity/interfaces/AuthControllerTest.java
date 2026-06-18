package com.notify.identity.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.notify.shared.infrastructure.ratelimit.RateLimitFilter;
import java.util.Map;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

class AuthControllerTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

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
        return "http://localhost:" + port + "/api/v1/auth";
    }

    @Test
    void givenValidRequest_whenRegister_thenReturns201AndTokens() {
        var request = Map.of("email", "newuser@test.com", "name", "New User", "password", "StrongP@ss1");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull().containsKey("accessToken").containsKey("refreshToken")
                .containsEntry("tokenType", "Bearer");
    }

    @Test
    void givenDuplicateEmail_whenRegister_thenReturns409() {
        var request = Map.of("email", "admin@notify.com", "name", "Duplicate", "password", "StrongP@ss1");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void givenInvalidEmail_whenRegister_thenReturns400() {
        var request = Map.of("email", "not-an-email", "name", "Bad Email", "password", "StrongP@ss1");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void givenWeakPassword_whenRegister_thenReturns400() {
        var request = Map.of("email", "weakpw@test.com", "name", "Weak Password", "password", "short");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void givenValidCredentials_whenLogin_thenReturns200AndTokens() {
        var request = Map.of("email", "admin@notify.com", "password", "Admin@123");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().containsKey("accessToken").containsKey("refreshToken")
                .containsEntry("tokenType", "Bearer");
    }

    @Test
    void givenInvalidPassword_whenLogin_thenReturns401() {
        var request = Map.of("email", "admin@notify.com", "password", "WrongP@ss1");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenNonexistentEmail_whenLogin_thenReturns401() {
        var request = Map.of("email", "nobody@test.com", "password", "SomeP@ss1");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenValidRefreshToken_whenRefresh_thenReturnsNewAccessToken() {
        var loginRequest = Map.of("email", "admin@notify.com", "password", "Admin@123");

        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(baseUrl() + "/login", loginRequest, Map.class);

        String refreshToken = (String) loginResponse.getBody().get("refreshToken");

        var refreshRequest = Map.of("refreshToken", refreshToken);
        ResponseEntity<Map> refreshResponse = restTemplate.postForEntity(baseUrl() + "/refresh", refreshRequest,
                Map.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull().containsKey("accessToken");
    }

    @Test
    void givenRefreshToken_whenReusedAfterRefresh_thenReturns401() {
        // Login
        var loginRequest = Map.of("email", "admin@notify.com", "password", "Admin@123");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(baseUrl() + "/login", loginRequest, Map.class);
        String originalRefreshToken = (String) loginResponse.getBody().get("refreshToken");

        // First refresh succeeds
        var firstRefreshRequest = Map.of("refreshToken", originalRefreshToken);
        ResponseEntity<Map> firstRefresh = restTemplate.postForEntity(baseUrl() + "/refresh", firstRefreshRequest,
                Map.class);
        assertThat(firstRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Second refresh with the same old token → 401 (rotation)
        ResponseEntity<Map> secondRefresh = restTemplate.postForEntity(baseUrl() + "/refresh", firstRefreshRequest,
                Map.class);
        assertThat(secondRefresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenValidRefreshToken_whenRefresh_thenReturnsNewRefreshToken() {
        var loginRequest = Map.of("email", "admin@notify.com", "password", "Admin@123");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(baseUrl() + "/login", loginRequest, Map.class);
        String refreshToken = (String) loginResponse.getBody().get("refreshToken");

        var refreshRequest = Map.of("refreshToken", refreshToken);
        ResponseEntity<Map> refreshResponse = restTemplate.postForEntity(baseUrl() + "/refresh", refreshRequest,
                Map.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull().containsKey("accessToken").containsKey("refreshToken");
        assertThat(refreshResponse.getBody().get("refreshToken")).isNotEqualTo(refreshToken);
    }

    @Test
    void givenInvalidRefreshToken_whenRefresh_thenReturns401() {
        var request = Map.of("refreshToken", "invalid-token");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/refresh", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenValidCurrentPassword_whenChangePassword_thenReturns200() {
        var loginRequest = Map.of("email", "admin@notify.com", "password", "Admin@123");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(baseUrl() + "/login", loginRequest, Map.class);
        String token = "Bearer " + loginResponse.getBody().get("accessToken");
        String oldRefreshToken = (String) loginResponse.getBody().get("refreshToken");

        var changeRequest = Map.of("currentPassword", "Admin@123", "newPassword", "NewP@ss1");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        var entity = new HttpEntity<>(changeRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/password", HttpMethod.PATCH, entity,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().containsKey("accessToken").containsKey("refreshToken")
                .containsEntry("tokenType", "Bearer");

        String newRefreshToken = (String) response.getBody().get("refreshToken");
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        var oldRefreshRequest = Map.of("refreshToken", oldRefreshToken);
        ResponseEntity<Map> oldRefreshResponse = restTemplate.postForEntity(baseUrl() + "/refresh", oldRefreshRequest,
                Map.class);
        assertThat(oldRefreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var newLoginRequest = Map.of("email", "admin@notify.com", "password", "NewP@ss1");
        ResponseEntity<Map> newLoginResponse = restTemplate.postForEntity(baseUrl() + "/login", newLoginRequest,
                Map.class);
        assertThat(newLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        var resetRequest = Map.of("currentPassword", "NewP@ss1", "newPassword", "Admin@123");
        var newToken = "Bearer " + newLoginResponse.getBody().get("accessToken");
        var resetHeaders = new HttpHeaders();
        resetHeaders.set("Authorization", newToken);
        var resetEntity = new HttpEntity<>(resetRequest, resetHeaders);
        restTemplate.exchange(baseUrl() + "/password", HttpMethod.PATCH, resetEntity, Map.class);
    }

    @Test
    void givenInvalidCurrentPassword_whenChangePassword_thenReturns401() {
        var loginRequest = Map.of("email", "admin@notify.com", "password", "Admin@123");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(baseUrl() + "/login", loginRequest, Map.class);
        String token = "Bearer " + loginResponse.getBody().get("accessToken");

        var changeRequest = Map.of("currentPassword", "WrongP@ss1", "newPassword", "NewP@ss1");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        var entity = new HttpEntity<>(changeRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/password", HttpMethod.PATCH, entity,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenUnauthenticated_whenChangePassword_thenReturns401() {
        var changeRequest = Map.of("currentPassword", "Admin@123", "newPassword", "NewP@ss1");
        var headers = new HttpHeaders();
        var entity = new HttpEntity<>(changeRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/password", HttpMethod.PATCH, entity,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenWeakNewPassword_whenChangePassword_thenReturns400() {
        var loginRequest = Map.of("email", "admin@notify.com", "password", "Admin@123");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(baseUrl() + "/login", loginRequest, Map.class);
        String token = "Bearer " + loginResponse.getBody().get("accessToken");

        var changeRequest = Map.of("currentPassword", "Admin@123", "newPassword", "short");
        var headers = new HttpHeaders();
        headers.set("Authorization", token);
        var entity = new HttpEntity<>(changeRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/password", HttpMethod.PATCH, entity,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void givenExceededRateLimit_whenLogin_thenReturns429() {
        var request = Map.of("email", "ratelimit@test.com", "password", "WrongP@ss1");

        ResponseEntity<Map> lastResponse = null;
        for (int i = 0; i < 6; i++) {
            lastResponse = restTemplate.postForEntity(baseUrl() + "/login", request, Map.class);
        }

        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
