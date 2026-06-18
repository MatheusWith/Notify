package com.notify.identity.domain.repository;

import com.notify.identity.domain.model.Email;
import com.notify.identity.domain.model.User;
import com.notify.identity.domain.model.UserId;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository {

    Optional<User> findById(UserId id);

    User save(User user);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    Optional<User> findByEmailAndEnabledTrue(Email email);

    Page<User> findAll(Pageable pageable);
}
