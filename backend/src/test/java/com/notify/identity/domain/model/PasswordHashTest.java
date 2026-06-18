package com.notify.identity.domain.model;

import static org.assertj.core.api.Assertions.*;

import com.notify.shared.domain.InvalidPasswordException;
import org.junit.jupiter.api.Test;

class PasswordHashTest {

    private static final String VALID_BCRYPT_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Test
    void givenValidBCryptHash_whenCreated_thenSucceeds() {
        PasswordHash hash = new PasswordHash(VALID_BCRYPT_HASH);

        assertThat(hash.value()).isEqualTo(VALID_BCRYPT_HASH);
    }

    @Test
    void givenNullHash_whenCreated_thenThrowsException() {
        assertThatThrownBy(() -> new PasswordHash(null)).isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("must not be null or blank");
    }

    @Test
    void givenBlankHash_whenCreated_thenThrowsException() {
        assertThatThrownBy(() -> new PasswordHash("")).isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("must not be null or blank");
    }

    @Test
    void givenInvalidHashFormat_whenCreated_thenThrowsException() {
        assertThatThrownBy(() -> new PasswordHash("not-a-bcrypt-hash")).isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Invalid BCrypt hash format");
    }

    @Test
    void givenTwoEqualHashes_thenTheyAreEqual() {
        PasswordHash hash1 = new PasswordHash(VALID_BCRYPT_HASH);
        PasswordHash hash2 = new PasswordHash(VALID_BCRYPT_HASH);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1.hashCode()).isEqualTo(hash2.hashCode());
    }

    @Test
    void givenDifferentHashes_thenTheyAreNotEqual() {
        PasswordHash hash1 = new PasswordHash(VALID_BCRYPT_HASH);
        PasswordHash hash2 = new PasswordHash("$2b$12$LJ3m4ys3Lk0TSwHnbfOMiOXPm1QkVRQK1GZg1uS5k.YGxOq0K7fqS");

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
