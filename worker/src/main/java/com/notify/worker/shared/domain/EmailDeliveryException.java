package com.notify.worker.shared.domain;

public class EmailDeliveryException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailDeliveryException(String message) {
        super(message);
    }
}
