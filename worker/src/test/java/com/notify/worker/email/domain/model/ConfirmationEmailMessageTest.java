package com.notify.worker.email.domain.model;

import static org.assertj.core.api.Assertions.*;

import com.notify.worker.shared.domain.BusinessException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfirmationEmailMessageTest {

    private final UUID messageId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final String email = "test@example.com";
    private final UUID token = UUID.randomUUID();
    private final LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);

    @Test
    void givenAllValidFields_whenCreated_thenSucceeds() {
        var msg = new ConfirmationEmailMessage(messageId, subscriptionId, email, token, expiresAt);

        assertThat(msg.messageId()).isEqualTo(messageId);
        assertThat(msg.subscriptionId()).isEqualTo(subscriptionId);
        assertThat(msg.email()).isEqualTo(email);
        assertThat(msg.token()).isEqualTo(token);
        assertThat(msg.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void givenNullMessageId_whenCreated_thenThrowsBusinessException() {
        assertThatThrownBy(() -> new ConfirmationEmailMessage(null, subscriptionId, email, token, expiresAt))
                .isInstanceOf(BusinessException.class).hasMessageContaining("messageId");
    }

    @Test
    void givenNullSubscriptionId_whenCreated_thenThrowsBusinessException() {
        assertThatThrownBy(() -> new ConfirmationEmailMessage(messageId, null, email, token, expiresAt))
                .isInstanceOf(BusinessException.class).hasMessageContaining("subscriptionId");
    }

    @Test
    void givenNullEmail_whenCreated_thenThrowsBusinessException() {
        assertThatThrownBy(() -> new ConfirmationEmailMessage(messageId, subscriptionId, null, token, expiresAt))
                .isInstanceOf(BusinessException.class).hasMessageContaining("email");
    }

    @Test
    void givenBlankEmail_whenCreated_thenThrowsBusinessException() {
        assertThatThrownBy(() -> new ConfirmationEmailMessage(messageId, subscriptionId, "", token, expiresAt))
                .isInstanceOf(BusinessException.class).hasMessageContaining("email");
    }

    @Test
    void givenNullToken_whenCreated_thenThrowsBusinessException() {
        assertThatThrownBy(() -> new ConfirmationEmailMessage(messageId, subscriptionId, email, null, expiresAt))
                .isInstanceOf(BusinessException.class).hasMessageContaining("token");
    }

    @Test
    void givenNullExpiresAt_whenCreated_thenThrowsBusinessException() {
        assertThatThrownBy(() -> new ConfirmationEmailMessage(messageId, subscriptionId, email, token, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("expiresAt");
    }
}
