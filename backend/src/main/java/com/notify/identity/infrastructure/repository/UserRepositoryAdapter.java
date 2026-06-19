package com.notify.identity.infrastructure.repository;

import com.notify.identity.domain.model.Email;
import com.notify.identity.domain.model.User;
import com.notify.identity.domain.model.UserId;
import com.notify.identity.domain.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final UserDomainMapper mapper;

    @Override
    public Optional<User> findById(UserId id) {
        return userJpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        Set<JpaRoleEntity> entityRoles = resolveRoles(user);
        JpaUserEntity entity = mapper.toJpa(user, entityRoles);
        entity = userJpaRepository.save(entity);
        return mapper.toDomain(entity);
    }

    private Set<JpaRoleEntity> resolveRoles(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return Set.of();
        }
        return user.getRoles().stream()
                .map(name -> roleJpaRepository.findByName(name)
                        .orElseThrow(() -> new IllegalStateException("Role not found: " + name)))
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return userJpaRepository.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return userJpaRepository.existsByEmail(email.value());
    }

    @Override
    public Optional<User> findByEmailAndEnabledTrue(Email email) {
        return userJpaRepository.findByEmailAndEnabledTrue(email.value()).map(mapper::toDomain);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userJpaRepository.findAll(pageable).map(mapper::toDomain);
    }
}
