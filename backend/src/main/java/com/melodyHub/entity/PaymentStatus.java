package com.melodyHub.entity;

public enum PaymentStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    EXPIRED;

    public static PaymentStatus fromDatabaseValue(String value) {
        return value == null ? PENDING : PaymentStatus.valueOf(value);
    }
}
