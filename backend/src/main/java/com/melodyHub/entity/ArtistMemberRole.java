package com.melodyHub.entity;

/**
 * Membership role within an Artist profile.
 *
 * <p>MVP implements OWNER and MANAGER. The enum is designed to be extended
 * to EDITOR and VIEWER in future releases without changes to the authorization
 * layer or database schema.</p>
 *
 * <p>Permissions:</p>
 * <ul>
 *   <li><b>OWNER</b>: Full control — edit profile, manage music, publish songs,
 *       manage team members, transfer ownership.</li>
 *   <li><b>MANAGER</b>: Edit profile, manage songs/lyrics, view dashboard and stats.
 *       Cannot remove OWNERs or transfer ownership.</li>
 * </ul>
 */
public enum ArtistMemberRole {
    OWNER,
    MANAGER;

    public static ArtistMemberRole fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return OWNER;
        }
        return ArtistMemberRole.valueOf(value.trim().toUpperCase());
    }
}
