package com.notify.identity.infrastructure.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<JpaUserEntity, Long> {

    Optional<JpaUserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<JpaUserEntity> findByEmailAndEnabledTrue(String email);
}
