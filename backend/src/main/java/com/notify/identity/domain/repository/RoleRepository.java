package com.notify.identity.domain.repository;

import com.notify.identity.domain.model.Role;
import com.notify.identity.domain.model.RoleName;
import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findById(Long id);

    Optional<Role> findByName(RoleName name);
}
