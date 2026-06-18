package com.notify.newsletter.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SubscribeRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

        String slug) {
}
