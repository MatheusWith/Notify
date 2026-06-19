package com.notify.worker.email.domain.model;

import java.util.UUID;

public interface EmailMessage {
    UUID messageId();
    String recipientEmail();
    String subject();
    String body();
}
