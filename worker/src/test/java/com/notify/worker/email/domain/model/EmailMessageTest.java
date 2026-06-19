package com.notify.worker.email.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmailMessageTest {

    @Test
    void givenConfirmationMessage_whenImplementsEmailMessage_thenReturnsCorrectValues() {
        var messageId = UUID.randomUUID();
        var subscriptionId = UUID.randomUUID();
        var email = "test@example.com";
        var token = UUID.randomUUID();
        var expiresAt = LocalDateTime.now().plusDays(1);

        EmailMessage msg = new ConfirmationEmailMessage(messageId, subscriptionId, email, token, expiresAt);

        assertThat(msg.messageId()).isEqualTo(messageId);
        assertThat(msg.recipientEmail()).isEqualTo(email);
        assertThat(msg.subject()).isNotBlank();
        assertThat(msg.body()).isNotBlank();
        assertThat(msg.body()).contains(token.toString());
    }
}
