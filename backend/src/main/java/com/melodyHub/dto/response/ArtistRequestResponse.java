package com.melodyHub.dto.response;

import com.melodyHub.entity.ArtistRequest;
import com.melodyHub.entity.ArtistRequestStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Artist request as seen by its owner (the requesting user).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistRequestResponse {
    private Integer id;
    private String artistName;
    private String slug;
    private String bio;
    private String imageUrl;
    private ArtistRequestStatus status;
    private String reviewNote;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    public static ArtistRequestResponse fromEntity(ArtistRequest request) {
        if (request == null) {
            return null;
        }

        return new ArtistRequestResponse(
                request.getId(),
                request.getArtistName(),
                request.getSlug(),
                request.getBio(),
                request.getImageUrl(),
                request.getStatus(),
                request.getReviewNote(),
                request.getReviewedAt(),
                request.getCreatedAt()
        );
    }
}
