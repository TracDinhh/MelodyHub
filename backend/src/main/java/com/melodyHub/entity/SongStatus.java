package com.melodyHub.entity;

public enum SongStatus {
    DRAFT,
    PUBLISHED,
    HIDDEN;

    public static SongStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return PUBLISHED;
        }

        return SongStatus.valueOf(value.trim().toUpperCase());
    }
}
