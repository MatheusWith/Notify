package com.notify.identity.application.port.in;

import com.notify.identity.application.dto.UpdateUserRequest;
import com.notify.identity.application.dto.UserResponse;
import com.notify.identity.domain.model.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserUseCase {

    UserResponse findById(UserId id);

    UserResponse findByEmail(String email);

    UserResponse updateProfile(UserId userId, UpdateUserRequest request);

    Page<UserResponse> findAll(Pageable pageable);

    void toggleUserStatus(UserId userId);
}
