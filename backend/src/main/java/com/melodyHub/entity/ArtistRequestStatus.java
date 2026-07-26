package com.melodyHub.entity;

public enum ArtistRequestStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static ArtistRequestStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }

        return ArtistRequestStatus.valueOf(value.trim().toUpperCase());
    }
}
