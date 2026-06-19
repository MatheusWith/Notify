package com.notify.newsletter.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Subscription {

    private UUID id;

    private SubscriberEmail email;

    private Long subscriberId;

    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    private UUID newsletterId;

    private UUID token;

    private LocalDateTime expiresAt;

    private LocalDateTime confirmedAt;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isConfirmed() {
        return status == SubscriptionStatus.CONFIRMED;
    }

    public void confirm() {
        if (isConfirmed()) {
            throw new IllegalStateException("Subscription is already confirmed");
        }
        if (isExpired()) {
            throw new IllegalStateException("Confirmation token has expired");
        }
        this.status = SubscriptionStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void refreshToken(UUID newToken, LocalDateTime newExpiresAt) {
        this.token = newToken;
        this.expiresAt = newExpiresAt;
        this.status = SubscriptionStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
    }
}
