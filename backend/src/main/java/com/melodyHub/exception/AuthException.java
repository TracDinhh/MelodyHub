package com.melodyHub.exception;

import lombok.Getter;

@Getter
public class AuthException extends Exception {
    private final String code;

    public AuthException(String code, String message) {
        super(message);
        this.code = code;
    }
}
