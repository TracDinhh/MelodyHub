package com.melodyHub.entity;

/**
 * Lifecycle status of an {@link ArtistAccessRequest}.
 *
 * <p>Valid transitions:</p>
 * <ul>
 *   <li>PENDING → APPROVED (admin approves in a single DB transaction)</li>
 *   <li>PENDING → REJECTED (admin rejects with an optional review note)</li>
 * </ul>
 * <p>APPROVED and REJECTED are terminal states. Requests in those states
 * cannot be re-processed.</p>
 */
public enum ArtistAccessRequestStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static ArtistAccessRequestStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        return ArtistAccessRequestStatus.valueOf(value.trim().toUpperCase());
    }
}
