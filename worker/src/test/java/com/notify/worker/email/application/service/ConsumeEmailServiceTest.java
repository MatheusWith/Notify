package com.notify.worker.email.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.notify.worker.email.application.port.out.SendEmailPort;
import com.notify.worker.email.domain.model.ConfirmationEmailMessage;
import com.notify.worker.email.domain.model.EmailDeliveryResult;
import com.notify.worker.email.domain.service.EmailSendingService;
import com.notify.worker.shared.domain.BusinessException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsumeEmailServiceTest {

    @Mock
    private SendEmailPort sendEmailPort;

    private EmailSendingService emailSendingService;
    private ConsumeEmailService service;

    private ConfirmationEmailMessage validMessage;
    private ConfirmationEmailMessage invalidMessage;

    @BeforeEach
    void setUp() {
        emailSendingService = new EmailSendingService();
        service = new ConsumeEmailService(sendEmailPort, emailSendingService);

        validMessage = new ConfirmationEmailMessage(UUID.randomUUID(), UUID.randomUUID(), "test@example.com",
                UUID.randomUUID(), LocalDateTime.now().plusDays(1));

        invalidMessage = new ConfirmationEmailMessage(UUID.randomUUID(), UUID.randomUUID(), "invalid-email",
                UUID.randomUUID(), LocalDateTime.now().plusDays(1));
    }

    @Test
    void givenValidMessage_whenProcess_thenSendsEmail() {
        var result = EmailDeliveryResult.success(validMessage.messageId(), validMessage.recipientEmail());
        when(sendEmailPort.send(validMessage)).thenReturn(result);

        service.process(validMessage);

        verify(sendEmailPort, times(1)).send(validMessage);
    }

    @Test
    void givenSendThrowsException_whenProcess_thenExceptionPropagates() {
        when(sendEmailPort.send(validMessage)).thenThrow(new RuntimeException("SMTP timeout"));

        assertThatThrownBy(() -> service.process(validMessage)).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SMTP timeout");

        verify(sendEmailPort, times(1)).send(validMessage);
    }

    @Test
    void givenInvalidEmail_whenProcess_thenThrowsException() {
        assertThatThrownBy(() -> service.process(invalidMessage)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid email format");

        verify(sendEmailPort, never()).send(any());
    }

    @Test
    void givenSendReturnsFailure_whenProcess_thenThrowsException() {
        var failure = EmailDeliveryResult.failure(validMessage.messageId(), validMessage.recipientEmail(),
                "SMTP rejected");
        when(sendEmailPort.send(validMessage)).thenReturn(failure);

        assertThatThrownBy(() -> service.process(validMessage)).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SMTP rejected");

        verify(sendEmailPort, times(1)).send(validMessage);
    }
}
