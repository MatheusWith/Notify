package com.notify.worker.email.domain.service;

import static org.assertj.core.api.Assertions.*;

import com.notify.worker.shared.domain.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailSendingServiceTest {

    private EmailSendingService service;

    @BeforeEach
    void setUp() {
        service = new EmailSendingService();
    }

    @Test
    void givenValidEmail_whenValidate_thenReturnsTrue() {
        assertThatCode(() -> service.validateEmail("user@example.com")).doesNotThrowAnyException();
    }

    @Test
    void givenInvalidEmail_whenValidate_thenThrowsException() {
        assertThatThrownBy(() -> service.validateEmail("not-an-email")).isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid email format");
    }

    @Test
    void givenNullEmail_whenValidate_thenThrowsException() {
        assertThatThrownBy(() -> service.validateEmail(null)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("email");
    }

    @Test
    void givenConfirmationMessage_whenBuildContent_thenContainsToken() {
        var token = UUID.randomUUID().toString();
        var content = service.buildConfirmationContent(token, "2025-12-31T23:59:59");

        assertThat(content).contains(token);
        assertThat(content).contains("Confirm your subscription");
    }

    @Test
    void givenConfirmationMessage_whenBuildContent_thenContainsExpiration() {
        var token = UUID.randomUUID().toString();
        var content = service.buildConfirmationContent(token, "2025-12-31T23:59:59");

        assertThat(content).contains("2025-12-31T23:59:59");
    }

    @Test
    void givenValidService_whenBuildSubject_thenReturnsNonBlank() {
        var subject = service.buildSubject();

        assertThat(subject).isNotBlank();
        assertThat(subject).contains("Notify");
    }
}
