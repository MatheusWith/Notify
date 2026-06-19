package com.notify.newsletter.interfaces;

import com.notify.NotifyApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = NotifyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractNewsletterIntegrationTest {

    private static final int RABBITMQ_AMQP_PORT = 5672;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notify_test").withUsername("test").withPassword("test");

    @Container
    static GenericContainer<?> rabbitmq = new GenericContainer<>("rabbitmq:4-management-alpine")
            .withExposedPorts(RABBITMQ_AMQP_PORT).withEnv("RABBITMQ_DEFAULT_USER", "test")
            .withEnv("RABBITMQ_DEFAULT_PASS", "test");

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
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbitmq.getMappedPort(RABBITMQ_AMQP_PORT));
        registry.add("spring.rabbitmq.username", () -> "test");
        registry.add("spring.rabbitmq.password", () -> "test");
        registry.add("app.jwt.secret",
                () -> "YTFkMmMzZDRlNWY2YTFiMmMzZDRlNWY2YTFiMmMzZDRlNWY2YTFiMmMzZDRlNWY2YTFiMmMzZDRlNWY2YTFiMg==");
        registry.add("app.jwt.access-token-expiration", () -> "900000");
        registry.add("app.jwt.refresh-token-expiration", () -> "604800000");
        registry.add("app.newsletter.confirmation-token-expiration-hours", () -> "24");
    }
}
