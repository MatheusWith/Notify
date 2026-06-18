package com.notify.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.notify.identity.interfaces.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class HealthControllerTest extends AbstractIntegrationTest {

    @Test
    void givenServerRunning_whenGetHealth_thenReturns200() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:" + port + "/api/v1/health";

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().containsEntry("status", "UP");
    }
}
