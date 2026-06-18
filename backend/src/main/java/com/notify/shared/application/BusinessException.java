package com.notify.shared.application;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }
}
