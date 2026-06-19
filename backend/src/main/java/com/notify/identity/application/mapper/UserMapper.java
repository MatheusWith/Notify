package com.notify.identity.application.mapper;

import com.notify.identity.application.dto.UserResponse;
import com.notify.identity.domain.model.User;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId() != null ? user.getId().value() : null, user.getEmail().value(),
                user.getName(), user.getRoles().stream().map(Enum::name).collect(Collectors.toList()), user.isEnabled(),
                user.getCreatedAt());
    }
}
