package com.notify.identity.application.port.in;

import com.notify.identity.application.dto.AuthResponse;
import com.notify.identity.application.dto.ChangePasswordRequest;
import com.notify.identity.application.dto.LoginRequest;
import com.notify.identity.application.dto.RefreshTokenRequest;
import com.notify.identity.application.dto.RegisterRequest;
import com.notify.identity.domain.model.UserId;

public interface AuthUseCase {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse changePassword(UserId userId, ChangePasswordRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);
}
