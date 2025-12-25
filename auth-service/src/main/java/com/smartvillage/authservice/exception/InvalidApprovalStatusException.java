package com.smartvillage.authservice.exception;

public class InvalidApprovalStatusException extends RuntimeException {
    public InvalidApprovalStatusException(String message) {
        super(message);
    }

    public InvalidApprovalStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
