package com.notify.identity.infrastructure.repository;

import com.notify.identity.domain.model.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<JpaRoleEntity, Long> {

    Optional<JpaRoleEntity> findByName(RoleName name);
}
