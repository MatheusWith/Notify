package com.notify.newsletter.application.dto;

import java.time.LocalDateTime;

public record SubscriberResponse(
    String name,
    String status,
    LocalDateTime createdAt
) {}
