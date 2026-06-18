package com.notify.identity.application.service;

import com.notify.identity.application.dto.AuthResponse;
import com.notify.identity.application.dto.ChangePasswordRequest;
import com.notify.identity.application.dto.LoginRequest;
import com.notify.identity.application.dto.RefreshTokenRequest;
import com.notify.identity.application.dto.RegisterRequest;
import com.notify.identity.application.port.in.AuthUseCase;
import com.notify.identity.domain.event.PasswordChanged;
import com.notify.identity.domain.event.UserRegistered;
import com.notify.identity.domain.model.Email;
import com.notify.identity.domain.model.PasswordHash;
import com.notify.identity.domain.model.RoleName;
import com.notify.identity.domain.model.User;
import com.notify.identity.domain.model.UserId;
import com.notify.identity.domain.repository.UserRepository;
import com.notify.identity.domain.service.PasswordService;
import com.notify.identity.infrastructure.security.JwtTokenProvider;
import com.notify.shared.application.AuthException;
import com.notify.shared.application.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthApplicationService implements AuthUseCase {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String USER_NOT_FOUND = "User not found";

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordService passwordService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public AuthResponse register(RegisterRequest request) {
        Email email = new Email(request.email());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(409, "Email already registered");
        }
        PasswordHash passwordHash = passwordService.hash(request.password());

        User user = User.builder().email(email).name(request.name().trim()).password(passwordHash).build();

        user.getRoles().add(RoleName.USER);
        user = userRepository.save(user);

        eventPublisher.publishEvent(new UserRegistered(user.getId(), user.getEmail().value(), user.getName()));

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, TOKEN_TYPE, 900);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(new Email(request.email()))
                .orElseThrow(() -> new AuthException(401, "Invalid email or password"));

        if (!user.isEnabled()) {
            throw new AuthException(401, "Account is disabled");
        }

        if (!passwordService.matches(request.password(), user.getPassword())) {
            throw new AuthException(401, "Invalid email or password");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, TOKEN_TYPE, 900);
    }

    @Override
    public AuthResponse changePassword(UserId userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AuthException(404, USER_NOT_FOUND));

        if (!passwordService.matches(request.currentPassword(), user.getPassword())) {
            throw new AuthException(401, "Current password is incorrect");
        }

        PasswordHash newPasswordHash = passwordService.hash(request.newPassword());
        user.setPassword(newPasswordHash);
        user.incrementTokenVersion();
        user = userRepository.save(user);

        eventPublisher.publishEvent(new PasswordChanged(userId));

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, TOKEN_TYPE, 900);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new AuthException(401, "Invalid or expired refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken());
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new AuthException(401, USER_NOT_FOUND));

        if (!user.isEnabled()) {
            throw new AuthException(401, "Account is disabled");
        }

        Long tokenVersion = jwtTokenProvider.getTokenVersionFromToken(request.refreshToken());
        if (!tokenVersion.equals(user.getTokenVersion())) {
            throw new AuthException(401, "Refresh token has been revoked");
        }

        user.incrementTokenVersion();
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, "Bearer", 900);
    }
}
