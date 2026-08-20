package com.melodyHub.entity;

public enum SongStatus {
    DRAFT,
    SUBMITTED,
    PUBLISHED,
    REJECTED,
    HIDDEN;

    public static SongStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return DRAFT;
        }

        return SongStatus.valueOf(value.trim().toUpperCase());
    }

    /** True when an artist may edit the song in its current state. */
    public boolean isEditableByArtist() {
        return this == DRAFT || this == REJECTED;
    }

    /** True when the artist may submit the song for review. */
    public boolean isSubmittable() {
        return this == DRAFT || this == REJECTED;
    }
}
