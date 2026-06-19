package com.notify.worker.email.interfaces.messaging;

import static org.mockito.Mockito.*;

import com.notify.worker.email.application.port.in.ConsumeEmailUseCase;
import com.notify.worker.email.domain.model.ConfirmationEmailMessage;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfirmationEmailListenerTest {

    @Mock
    private ConsumeEmailUseCase consumeEmailUseCase;

    private ConfirmationEmailListener listener;

    @BeforeEach
    void setUp() {
        listener = new ConfirmationEmailListener(consumeEmailUseCase);
    }

    @Test
    void givenValidMessage_whenReceived_thenDelegatesToUseCase() {
        var message = new ConfirmationEmailMessage(UUID.randomUUID(), UUID.randomUUID(), "test@example.com",
                UUID.randomUUID(), LocalDateTime.now().plusDays(1));

        listener.handleConfirmationEmail(message);

        verify(consumeEmailUseCase, times(1)).process(message);
    }

    @Test
    void givenNullMessage_whenReceived_thenDoesNotDelegate() {
        listener.handleConfirmationEmail(null);

        verify(consumeEmailUseCase, never()).process(any());
    }
}
