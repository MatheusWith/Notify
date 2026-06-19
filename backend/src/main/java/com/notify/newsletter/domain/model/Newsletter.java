package com.notify.newsletter.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Newsletter {

    private UUID id;

    private Long senderId;

    private String name;

    private Slug slug;

    @Builder.Default
    private String description = "";

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
