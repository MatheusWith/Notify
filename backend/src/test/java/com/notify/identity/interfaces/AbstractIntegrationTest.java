package com.notify.identity.interfaces;

import com.notify.NotifyApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = NotifyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notify_test").withUsername("test").withPassword("test");

    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.profiles.active", () -> "dev");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("app.jwt.secret",
                () -> "YTFkMmMzZDRlNWY2YTFiMmMzZDRlNWY2YTFiMmMzZDRlNWY2YTFiMmMzZDRlNWY2YTFiMmMzZDRlNWY2YTFiMg==");
        registry.add("app.jwt.access-token-expiration", () -> "900000");
        registry.add("app.jwt.refresh-token-expiration", () -> "604800000");
    }
}
