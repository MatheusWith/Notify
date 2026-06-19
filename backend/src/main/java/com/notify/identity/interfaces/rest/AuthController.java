package com.notify.identity.interfaces.rest;

import com.notify.identity.application.dto.*;
import com.notify.identity.application.port.in.AuthUseCase;
import com.notify.identity.domain.model.UserId;
import com.notify.identity.infrastructure.security.UserPrincipal;
import com.notify.identity.interfaces.resolver.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/password")
    public ResponseEntity<AuthResponse> changePassword(@CurrentUser UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        AuthResponse response = authService.changePassword(UserId.of(principal.userId()), request);
        return ResponseEntity.ok(response);
    }
}
