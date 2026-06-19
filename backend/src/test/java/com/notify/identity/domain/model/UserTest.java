package com.notify.identity.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final String VALID_BCRYPT_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private Email email;
    private PasswordHash passwordHash;

    @BeforeEach
    void setUp() {
        email = new Email("user@example.com");
        passwordHash = new PasswordHash(VALID_BCRYPT_HASH);
    }

    @Test
    void givenValidData_whenBuilt_thenSucceeds() {
        User user = User.builder().email(email).name("John Doe").password(passwordHash).build();

        assertThat(user.getId()).isNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getName()).isEqualTo("John Doe");
        assertThat(user.getPassword()).isEqualTo(passwordHash);
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getTokenVersion()).isZero();
        assertThat(user.getRoles()).isEmpty();
    }

    @Test
    void givenDefaultUser_whenDisabled_thenEnabledIsFalse() {
        User user = createDefaultUser();

        user.disable();

        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void givenDisabledUser_whenEnabled_thenEnabledIsTrue() {
        User user = createDefaultUser();
        user.disable();

        user.enable();

        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void givenUser_whenIncrementTokenVersion_thenVersionIncrements() {
        User user = createDefaultUser();

        Long initialVersion = user.getTokenVersion();
        user.incrementTokenVersion();

        assertThat(user.getTokenVersion()).isEqualTo(initialVersion + 1);
    }

    @Test
    void givenUser_whenUpdateProfile_thenNameAndEmailChange() {
        User user = createDefaultUser();
        Email newEmail = new Email("newemail@example.com");

        user.updateProfile("Jane Doe", newEmail);

        assertThat(user.getName()).isEqualTo("Jane Doe");
        assertThat(user.getEmail()).isEqualTo(newEmail);
    }

    @Test
    void givenUserWithRole_whenHasRole_thenReturnsTrue() {
        User user = createDefaultUser();
        user.getRoles().add(RoleName.USER);

        assertThat(user.hasRole(RoleName.USER)).isTrue();
    }

    @Test
    void givenUserWithoutRole_whenHasRole_thenReturnsFalse() {
        User user = createDefaultUser();

        assertThat(user.hasRole(RoleName.USER)).isFalse();
    }

    @Test
    void givenUser_whenSetPassword_thenPasswordChanges() {
        User user = createDefaultUser();
        PasswordHash newHash = new PasswordHash(VALID_BCRYPT_HASH);

        user.setPassword(newHash);

        assertThat(user.getPassword()).isEqualTo(newHash);
    }

    private User createDefaultUser() {
        return User.builder().email(email).name("John Doe").password(passwordHash).build();
    }
}
