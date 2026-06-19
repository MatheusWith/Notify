package com.notify.newsletter.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscribeResponse(UUID id, String email, String status, LocalDateTime expiresAt) {
}
