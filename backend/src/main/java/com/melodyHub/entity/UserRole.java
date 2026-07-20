package com.melodyHub.entity;

public enum UserRole {
    USER,
    ARTIST,
    ADMIN;

    public static UserRole fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }

        return UserRole.valueOf(value.trim().toUpperCase());
    }
}
