package com.melodyHub.dto.response;

import com.melodyHub.entity.ArtistRequest;
import com.melodyHub.entity.ArtistRequestStatus;
import com.melodyHub.repository.ArtistRequestRepository;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Artist request enriched with requester info for the admin review queue.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistRequestAdminResponse {
    private Integer id;
    private Integer userId;
    private String requesterUsername;
    private String requesterDisplayName;
    private String requesterEmail;
    private String artistName;
    private String slug;
    private String bio;
    private String imageUrl;
    private ArtistRequestStatus status;
    private LocalDateTime createdAt;

    public static ArtistRequestAdminResponse fromRow(ArtistRequestRepository.AdminRow row) {
        if (row == null) {
            return null;
        }

        ArtistRequest request = row.request();
        return new ArtistRequestAdminResponse(
                request.getId(),
                request.getUserId(),
                row.username(),
                row.displayName(),
                row.email(),
                request.getArtistName(),
                request.getSlug(),
                request.getBio(),
                request.getImageUrl(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}
