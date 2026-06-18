package com.notify.identity.infrastructure.repository;

import com.notify.identity.domain.model.Email;
import com.notify.identity.domain.model.PasswordHash;
import com.notify.identity.domain.model.User;
import com.notify.identity.domain.model.UserId;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserDomainMapper {

    public JpaUserEntity toJpa(User domain, Set<JpaRoleEntity> resolvedRoles) {
        if (domain == null)
            return null;

        JpaUserEntity.JpaUserEntityBuilder builder = JpaUserEntity.builder()
                .id(domain.getId() != null ? domain.getId().value() : null).email(domain.getEmail().value())
                .name(domain.getName()).password(domain.getPassword().value()).enabled(domain.isEnabled())
                .tokenVersion(domain.getTokenVersion()).createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt());

        if (resolvedRoles != null && !resolvedRoles.isEmpty()) {
            builder.roles(resolvedRoles);
        }

        return builder.build();
    }

    public User toDomain(JpaUserEntity entity) {
        if (entity == null)
            return null;

        User.UserBuilder builder = User.builder().id(entity.getId() != null ? UserId.of(entity.getId()) : null)
                .email(new Email(entity.getEmail())).name(entity.getName())
                .password(new PasswordHash(entity.getPassword())).enabled(entity.isEnabled())
                .tokenVersion(entity.getTokenVersion()).createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        if (entity.getRoles() != null) {
            builder.roles(entity.getRoles().stream().map(JpaRoleEntity::getName).collect(Collectors.toSet()));
        }

        return builder.build();
    }
}
