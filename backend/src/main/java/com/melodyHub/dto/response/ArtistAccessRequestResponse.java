package com.melodyHub.dto.response;

import com.melodyHub.entity.ArtistAccessRequest;
import com.melodyHub.entity.ArtistAccessRequestStatus;
import com.melodyHub.entity.ArtistAccessRequestType;
import com.melodyHub.entity.ArtistRelationship;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User-facing response for an artist access request.
 * Returned by:
 * <ul>
 *   <li>{@code POST /api/artist-access-requests} (on submit)</li>
 *   <li>{@code GET  /api/artist-access-requests/me} (request history)</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistAccessRequestResponse {
    private Integer id;
    private ArtistAccessRequestType requestType;
    private ArtistRelationship relationship;

    /** For CLAIM_ARTIST: ID of the artist being claimed. */
    private Integer artistId;
    /** For CLAIM_ARTIST: name of the existing artist. May be populated by service. */
    private String existingArtistName;

    /** For CREATE_ARTIST: name requested for the new artist. */
    private String requestedArtistName;

    private ArtistAccessRequestStatus status;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ArtistAccessRequestResponse fromEntity(ArtistAccessRequest request) {
        return fromEntity(request, null);
    }

    public static ArtistAccessRequestResponse fromEntity(
            ArtistAccessRequest request,
            String existingArtistName
    ) {
        return new ArtistAccessRequestResponse(
                request.getId(),
                request.getRequestType(),
                request.getRelationship(),
                request.getArtistId(),
                existingArtistName,
                request.getRequestedArtistName(),
                request.getStatus(),
                request.getReviewNote(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
