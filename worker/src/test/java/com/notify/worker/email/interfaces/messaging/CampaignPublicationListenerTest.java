package com.notify.worker.email.interfaces.messaging;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.notify.worker.email.application.port.in.ConsumeCampaignUseCase;
import com.notify.worker.email.domain.model.CampaignPublishedMessage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignPublicationListenerTest {

    @Mock
    private ConsumeCampaignUseCase consumeCampaignUseCase;

    private CampaignPublicationListener listener;

    @BeforeEach
    void setUp() {
        listener = new CampaignPublicationListener(consumeCampaignUseCase);
    }

    @Test
    void givenValidMessage_whenReceived_thenDelegatesToUseCase() {
        var message = new CampaignPublishedMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "my-slug",
                "Subject", "content", List.of("user@example.com"));

        listener.handleCampaignPublication(message);

        verify(consumeCampaignUseCase, times(1)).process(message);
    }

    @Test
    void givenNullMessage_whenReceived_thenDoesNotDelegate() {
        listener.handleCampaignPublication(null);

        verify(consumeCampaignUseCase, never()).process(any());
    }
}
