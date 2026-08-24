package com.melodyHub.exception;

public class AlbumException extends Exception {
    private final String code;

    public AlbumException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
