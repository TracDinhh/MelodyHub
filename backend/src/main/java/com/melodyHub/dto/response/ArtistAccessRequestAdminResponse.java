package com.melodyHub.dto.response;

import com.melodyHub.entity.ArtistAccessRequest;
import com.melodyHub.entity.ArtistAccessRequestStatus;
import com.melodyHub.entity.ArtistAccessRequestType;
import com.melodyHub.entity.ArtistMemberRole;
import com.melodyHub.entity.ArtistRelationship;
import com.melodyHub.repository.ArtistAccessRequestRepository;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Enriched artist access request for the admin review queue.
 * Returned by {@code GET /api/admin/artist-access-requests}.
 *
 * <p>Includes:</p>
 * <ul>
 *   <li>Requester's account info (username, email)</li>
 *   <li>Request type (CLAIM vs CREATE)</li>
 *   <li>Relationship and resolved membership role (for admin preview)</li>
 *   <li>For CLAIM: existing artist name/slug</li>
 *   <li>For CREATE: requested artist name</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistAccessRequestAdminResponse {
    private Integer id;
    private Integer userId;
    private String requesterUsername;
    private String requesterDisplayName;
    private String requesterEmail;

    private ArtistAccessRequestType requestType;
    private ArtistRelationship relationship;

    /** The membership role that will be assigned on approval (preview for admin). */
    private ArtistMemberRole resolvedMemberRole;

    /** CLAIM_ARTIST: name of the existing artist being claimed. */
    private String existingArtistName;
    /** CLAIM_ARTIST: slug of the existing artist being claimed. */
    private String existingArtistSlug;

    /** CREATE_ARTIST: requested name for the new artist. */
    private String requestedArtistName;
    private String requestedBio;
    private String requestedImageUrl;

    private String websiteUrl;
    private String socialUrl;
    private String message;

    private ArtistAccessRequestStatus status;
    private String reviewNote;
    private LocalDateTime createdAt;

    /**
     * Builds an admin response from a repository row.
     *
     * @param row              joined row (request + requester info + optional existing artist)
     * @param resolvedMemberRole the role that would be assigned on approval
     */
    public static ArtistAccessRequestAdminResponse fromRow(
            ArtistAccessRequestRepository.AdminRow row,
            ArtistMemberRole resolvedMemberRole
    ) {
        if (row == null) {
            return null;
        }

        ArtistAccessRequest request = row.request();
        return new ArtistAccessRequestAdminResponse(
                request.getId(),
                request.getUserId(),
                row.username(),
                row.displayName(),
                row.email(),
                request.getRequestType(),
                request.getRelationship(),
                resolvedMemberRole,
                row.existingArtistName(),
                row.existingArtistSlug(),
                request.getRequestedArtistName(),
                request.getRequestedBio(),
                request.getRequestedImageUrl(),
                request.getWebsiteUrl(),
                request.getSocialUrl(),
                request.getMessage(),
                request.getStatus(),
                request.getReviewNote(),
                request.getCreatedAt()
        );
    }
}
