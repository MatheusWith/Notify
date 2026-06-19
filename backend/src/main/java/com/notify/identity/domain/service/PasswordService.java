package com.notify.identity.domain.service;

import com.notify.identity.domain.model.PasswordHash;
import com.notify.shared.domain.InvalidPasswordException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final String pepper;

    public PasswordService(@Value("${password.pepper}") String pepper) {
        this.pepper = pepper;
    }

    public PasswordHash hash(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8 || rawPassword.length() > 128) {
            throw new InvalidPasswordException("Password must be between 8 and 128 characters");
        }
        if (!rawPassword.matches(".*[a-zA-Z].*")) {
            throw new InvalidPasswordException("Password must contain at least one letter");
        }
        if (!rawPassword.matches(".*\\d.*")) {
            throw new InvalidPasswordException("Password must contain at least one number");
        }
        if (!rawPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new InvalidPasswordException("Password must contain at least one special character");
        }
        String hash = ENCODER.encode(pepper + rawPassword);
        return new PasswordHash(hash);
    }

    public boolean matches(String rawPassword, PasswordHash hash) {
        return ENCODER.matches(pepper + rawPassword, hash.value());
    }
}
