package com.melodyHub.exception;

import lombok.Getter;

@Getter
public class ArtistException extends Exception {
    private final String code;

    public ArtistException(String code, String message) {
        super(message);
        this.code = code;
    }
}
