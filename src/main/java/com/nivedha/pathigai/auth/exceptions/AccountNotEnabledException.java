package com.nivedha.pathigai.auth.exceptions;

public class AccountNotEnabledException extends RuntimeException {
    public AccountNotEnabledException(String message) {
        super(message);
    }

    public AccountNotEnabledException(String message, Throwable cause) {
        super(message, cause);
    }
}