package com.melodyHub.exception;

import lombok.Getter;

@Getter
public class SongException extends Exception {
    private final String code;

    public SongException(String code, String message) {
        super(message);
        this.code = code;
    }
}
