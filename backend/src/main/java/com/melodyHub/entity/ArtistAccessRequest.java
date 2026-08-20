package com.melodyHub.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An artist access request submitted by a user.
 *
 * <p>Two types:</p>
 * <ul>
 *   <li><b>CLAIM_ARTIST</b>: {@code artistId} is required; the user claims an existing
 *       artist profile. {@code requestedArtistName} must be {@code null}.</li>
 *   <li><b>CREATE_ARTIST</b>: {@code artistId} must be {@code null}; the user requests
 *       creation of a new artist profile. {@code requestedArtistName} is required.
 *       MVP restriction: {@code relationship} must be {@link ArtistRelationship#ARTIST}.</li>
 * </ul>
 *
 * <p>Admin approval is performed in a single DB transaction, resulting in
 * either a new {@code artist_members} row (CLAIM) or a new {@code artists} row
 * plus a new {@code artist_members} row (CREATE).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistAccessRequest {
    private Integer id;
    private Integer userId;

    /** Non-null for CLAIM_ARTIST; null for CREATE_ARTIST. */
    private Integer artistId;

    private ArtistAccessRequestType requestType;

    /** Non-null for CREATE_ARTIST; null for CLAIM_ARTIST. */
    private String requestedArtistName;
    private String requestedBio;
    private String requestedImageUrl;

    private ArtistRelationship relationship;

    /** Optional supporting information provided by the requester. */
    private String websiteUrl;
    private String socialUrl;
    private String message;

    private ArtistAccessRequestStatus status;
    private String reviewNote;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
