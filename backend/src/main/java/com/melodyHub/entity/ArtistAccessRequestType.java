package com.melodyHub.entity;

/**
 * Distinguishes the two kinds of Artist access requests.
 *
 * <ul>
 *   <li><b>CLAIM_ARTIST</b>: The user claims an existing artist profile.
 *       {@code artist_id} must be provided. The artist must exist and be active.
 *       The user must not already have a membership for that artist.</li>
 *
 *   <li><b>CREATE_ARTIST</b>: The user requests creation of a new artist profile.
 *       {@code artist_id} must be NULL. {@code requested_artist_name} is required.
 *       MVP restriction: {@code relationship} must be {@link ArtistRelationship#ARTIST}.</li>
 * </ul>
 */
public enum ArtistAccessRequestType {
    CLAIM_ARTIST,
    CREATE_ARTIST;

    public static ArtistAccessRequestType fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ArtistAccessRequestType value must not be blank");
        }
        return ArtistAccessRequestType.valueOf(value.trim().toUpperCase());
    }
}
