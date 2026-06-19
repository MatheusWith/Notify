package com.notify.identity.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(Long id, String email, String name, List<String> roles, boolean enabled,
        LocalDateTime createdAt) {

    public UserResponse(Long id, String email, String name, List<String> roles, boolean enabled,
            LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.roles = roles != null ? List.copyOf(roles) : List.of();
        this.enabled = enabled;
        this.createdAt = createdAt;
    }
}
