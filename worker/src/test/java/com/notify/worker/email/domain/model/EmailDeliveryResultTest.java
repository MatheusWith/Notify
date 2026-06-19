package com.notify.worker.email.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmailDeliveryResultTest {

    private final UUID messageId = UUID.randomUUID();
    private final String recipient = "test@example.com";

    @Test
    void givenSuccessResult_whenCreated_thenHasCorrectValues() {
        var result = EmailDeliveryResult.success(messageId, recipient);

        assertThat(result.messageId()).isEqualTo(messageId);
        assertThat(result.recipient()).isEqualTo(recipient);
        assertThat(result.success()).isTrue();
        assertThat(result.attemptedAt()).isNotNull();
    }

    @Test
    void givenFailureResult_whenCreated_thenHasErrorMessage() {
        var result = EmailDeliveryResult.failure(messageId, recipient, "SMTP error");

        assertThat(result.messageId()).isEqualTo(messageId);
        assertThat(result.recipient()).isEqualTo(recipient);
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("SMTP error");
        assertThat(result.attemptedAt()).isNotNull();
    }

    @Test
    void givenSuccessResult_whenCreated_thenErrorMessageIsNull() {
        var result = EmailDeliveryResult.success(messageId, recipient);

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
    }
}
