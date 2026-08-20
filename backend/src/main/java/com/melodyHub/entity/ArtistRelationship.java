package com.melodyHub.entity;

/**
 * Describes the requester's relationship to the artist they are claiming
 * or creating in an {@link ArtistAccessRequest}.
 *
 * <p>This is used during Admin approval to resolve the appropriate
 * {@link ArtistMemberRole} for the created membership:</p>
 * <ul>
 *   <li>ARTIST → OWNER</li>
 *   <li>MANAGER, LABEL, TEAM_MEMBER, OTHER → MANAGER</li>
 * </ul>
 *
 * <p>MVP restriction: {@code CREATE_ARTIST} requests only accept {@code ARTIST}.</p>
 */
public enum ArtistRelationship {
    ARTIST,
    MANAGER,
    LABEL,
    TEAM_MEMBER,
    OTHER;

    public static ArtistRelationship fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return ARTIST;
        }
        return ArtistRelationship.valueOf(value.trim().toUpperCase());
    }
}
