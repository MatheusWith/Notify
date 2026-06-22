package com.notify.worker.email.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notify.worker.email.application.port.out.SendEmailPort;
import com.notify.worker.email.domain.model.CampaignPublishedMessage;
import com.notify.worker.email.domain.model.EmailDeliveryResult;
import com.notify.worker.email.domain.service.EmailSendingService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsumeCampaignServiceTest {

    @Mock
    private SendEmailPort sendEmailPort;
    @Mock
    private EmailSendingService emailSendingService;

    private ConsumeCampaignService service;

    @BeforeEach
    void setUp() {
        service = new ConsumeCampaignService(sendEmailPort, emailSendingService);
    }

    @Test
    void givenThreeSubscribers_whenProcess_thenSendsThreeEmails() {
        var message = new CampaignPublishedMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "my-slug",
                "Subject", "content", List.of("user1@example.com", "user2@example.com", "user3@example.com"));

        when(sendEmailPort.send(any())).thenReturn(
                new EmailDeliveryResult(UUID.randomUUID(), true, "user1@example.com", null, LocalDateTime.now()));

        service.process(message);

        verify(sendEmailPort, times(3)).send(any());
    }

    @Test
    void givenMixedSuccessFailure_whenProcess_thenContinuesAfterFailure() {
        var message = new CampaignPublishedMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "my-slug",
                "Subject", "content", List.of("ok1@example.com", "ok2@example.com", "fail@example.com"));

        when(sendEmailPort.send(any()))
                .thenReturn(
                        new EmailDeliveryResult(UUID.randomUUID(), true, "ok1@example.com", null, LocalDateTime.now()))
                .thenReturn(
                        new EmailDeliveryResult(UUID.randomUUID(), true, "ok2@example.com", null, LocalDateTime.now()))
                .thenReturn(new EmailDeliveryResult(UUID.randomUUID(), false, "fail@example.com", "SMTP error",
                        LocalDateTime.now()));

        service.process(message);

        verify(sendEmailPort, times(3)).send(any());
    }

    @Test
    void givenOneInvalidEmail_whenProcess_thenSendsValidOnes() {
        var message = new CampaignPublishedMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "my-slug",
                "Subject", "content", List.of("valid1@example.com", "valid2@example.com"));

        when(sendEmailPort.send(any())).thenReturn(
                new EmailDeliveryResult(UUID.randomUUID(), true, "valid1@example.com", null, LocalDateTime.now()));

        service.process(message);

        verify(sendEmailPort, times(2)).send(any());
    }
}
