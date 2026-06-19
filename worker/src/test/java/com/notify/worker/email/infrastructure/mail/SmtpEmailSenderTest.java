package com.notify.worker.email.infrastructure.mail;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.notify.worker.email.domain.model.EmailMessage;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    @Mock
    private JavaMailSender javaMailSender;

    private SmtpEmailSender sender;

    @BeforeEach
    void setUp() {
        sender = new SmtpEmailSender(javaMailSender);
    }

    @Test
    void givenValidMessage_whenSend_thenReturnsSuccess() throws Exception {
        var messageId = UUID.randomUUID();
        EmailMessage msg = createTestMessage(messageId);

        var mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        var result = sender.send(msg);

        assertThat(result.success()).isTrue();
        assertThat(result.messageId()).isEqualTo(messageId);
        assertThat(result.recipient()).isEqualTo("test@example.com");
        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void givenSmtpUnavailable_whenSend_thenReturnsFailure() {
        EmailMessage msg = createTestMessage(UUID.randomUUID());

        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP connection refused"));

        var result = sender.send(msg);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("SMTP connection refused");
    }

    private EmailMessage createTestMessage(UUID messageId) {
        return new com.notify.worker.email.domain.model.ConfirmationEmailMessage(messageId, UUID.randomUUID(),
                "test@example.com", UUID.randomUUID(), LocalDateTime.now().plusDays(1));
    }
}
