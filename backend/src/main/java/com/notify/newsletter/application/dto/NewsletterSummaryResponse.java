package com.notify.newsletter.application.dto;

import java.util.UUID;

public record NewsletterSummaryResponse(UUID id, String name, String slug, long subscriberCount) {
}
