package com.notify.shared.application;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    public AuthException(int status, String message) {
        super(message);
        this.status = status;
    }
}
