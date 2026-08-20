package com.melodyHub.dto.request;

import com.melodyHub.entity.ArtistAccessRequestType;
import com.melodyHub.entity.ArtistRelationship;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for submitting an artist access request.
 * Sent to {@code POST /api/artist-access-requests}.
 *
 * <h2>CLAIM_ARTIST</h2>
 * <ul>
 *   <li>{@code requestType} = CLAIM_ARTIST</li>
 *   <li>{@code artistId} required (must reference existing, non-deleted artist)</li>
 *   <li>{@code requestedArtistName} must be null/blank</li>
 *   <li>{@code relationship} any value</li>
 * </ul>
 *
 * <h2>CREATE_ARTIST (MVP)</h2>
 * <ul>
 *   <li>{@code requestType} = CREATE_ARTIST</li>
 *   <li>{@code artistId} must be null</li>
 *   <li>{@code requestedArtistName} required</li>
 *   <li>{@code relationship} must be ARTIST (MVP restriction)</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistAccessRequestCreateRequest {
    private ArtistAccessRequestType requestType;

    /** CLAIM_ARTIST only: ID of the existing artist being claimed. */
    private Integer artistId;

    /** CREATE_ARTIST only: desired name for the new artist. */
    private String requestedArtistName;

    /** CREATE_ARTIST only: bio for the new artist. */
    private String requestedBio;

    /** CREATE_ARTIST only: image URL for the new artist. */
    private String requestedImageUrl;

    /** Requester's relationship to the artist. */
    private ArtistRelationship relationship;

    /** Optional: requester's website URL. */
    private String websiteUrl;

    /** Optional: requester's social media URL. */
    private String socialUrl;

    /** Optional: message to the admin reviewing this request. */
    private String message;
}
