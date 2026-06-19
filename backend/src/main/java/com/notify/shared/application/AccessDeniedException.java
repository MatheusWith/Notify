package com.notify.shared.application;

import lombok.Getter;

@Getter
public class AccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    public AccessDeniedException(int status, String message) {
        super(message);
        this.status = status;
    }
}
