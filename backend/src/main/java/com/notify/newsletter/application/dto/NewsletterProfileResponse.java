package com.notify.newsletter.application.dto;

import java.util.UUID;

public record NewsletterProfileResponse(UUID id, String name, String slug, String description, long subscriberCount) {
}
