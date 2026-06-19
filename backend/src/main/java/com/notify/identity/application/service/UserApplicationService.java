package com.notify.identity.application.service;

import com.notify.identity.application.dto.UpdateUserRequest;
import com.notify.identity.application.dto.UserResponse;
import com.notify.identity.application.mapper.UserMapper;
import com.notify.identity.application.port.in.UserUseCase;
import com.notify.identity.domain.model.Email;
import com.notify.identity.domain.model.User;
import com.notify.identity.domain.model.UserId;
import com.notify.identity.domain.repository.UserRepository;
import com.notify.shared.application.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UserApplicationService implements UserUseCase {

    private static final String USER_NOT_FOUND = "User not found";

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse findById(UserId id) {
        User user = userRepository.findById(id).orElseThrow(() -> new BusinessException(404, USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse findByEmail(String email) {
        User user = userRepository.findByEmail(new Email(email))
                .orElseThrow(() -> new BusinessException(404, USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateProfile(UserId userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(404, USER_NOT_FOUND));

        if (!user.getEmail().value().equals(request.email())) {
            if (userRepository.findByEmail(new Email(request.email())).isPresent()) {
                throw new BusinessException(409, "Email already in use");
            }
        }

        user.updateProfile(request.name().trim(), new Email(request.email()));
        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    public void toggleUserStatus(UserId userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(404, USER_NOT_FOUND));

        if (user.isEnabled()) {
            user.disable();
        } else {
            user.enable();
        }

        userRepository.save(user);
    }
}
