package com.notify.identity.domain.model;

import static org.assertj.core.api.Assertions.*;

import com.notify.shared.domain.InvalidEmailException;
import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void givenValidEmail_whenCreated_thenSucceeds() {
        Email email = new Email("test@example.com");

        assertThat(email.value()).isEqualTo("test@example.com");
    }

    @Test
    void givenEmailWithUppercase_whenCreated_thenLowercases() {
        Email email = new Email("Test@Example.Com");

        assertThat(email.value()).isEqualTo("test@example.com");
    }

    @Test
    void givenEmailWithSpaces_whenCreated_thenTrims() {
        Email email = new Email("  user@example.com  ");

        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @Test
    void givenNullEmail_whenCreated_thenThrowsException() {
        assertThatThrownBy(() -> new Email(null)).isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    void givenInvalidEmail_whenCreated_thenThrowsException() {
        assertThatThrownBy(() -> new Email("not-an-email")).isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    void givenBlankEmail_whenCreated_thenThrowsException() {
        assertThatThrownBy(() -> new Email("")).isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    void givenTwoEqualEmails_thenTheyAreEqual() {
        Email email1 = new Email("user@example.com");
        Email email2 = new Email("user@example.com");

        assertThat(email1).isEqualTo(email2);
        assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
    }

    @Test
    void givenDifferentEmails_thenTheyAreNotEqual() {
        Email email1 = new Email("user1@example.com");
        Email email2 = new Email("user2@example.com");

        assertThat(email1).isNotEqualTo(email2);
    }
}
