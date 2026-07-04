package com.melodyHub.entity;

public enum UserStatus {
    ACTIVE,
    BANNED;

    public static UserStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }

        return UserStatus.valueOf(value.trim().toUpperCase());
    }
}
