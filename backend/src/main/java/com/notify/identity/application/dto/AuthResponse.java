package com.notify.identity.application.dto;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, int expiresIn) {
}
