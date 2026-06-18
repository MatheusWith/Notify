package com.notify.identity.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(Long id, String email, String name, List<String> roles, boolean enabled,
        LocalDateTime createdAt) {

    public UserResponse {
        if (roles != null) {
            roles = List.copyOf(roles);
        }
    }
}
