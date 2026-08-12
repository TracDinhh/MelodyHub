package com.melodyHub.entity;

public enum LyricsType {
    PLAIN,
    SYNCED;

    public static LyricsType fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return PLAIN;
        }
        return LyricsType.valueOf(value.trim().toUpperCase());
    }
}
