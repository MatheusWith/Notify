package com.notify.identity.application.dto;

import com.notify.shared.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(@NotBlank(message = "Current password is required") String currentPassword,

        @NotBlank(message = "New password is required") @Size(min = 8, max = 128, message = "New password must be between 8 and 128 characters") @StrongPassword String newPassword) {
}
