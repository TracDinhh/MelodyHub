package com.melodyHub.dto.response;

import com.melodyHub.entity.Artist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight artist info embedded inside Song responses (detail page,
 * related songs, etc.). Drops bio, timestamps, and user_id — the caller
 * can fetch the full {@link ArtistPublicResponse} when needed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistSummaryResponse {
    private Integer id;
    private String name;
    private String slug;
    private String imageUrl;

    public static ArtistSummaryResponse fromEntity(Artist artist) {
        if (artist == null) {
            return null;
        }
        return new ArtistSummaryResponse(
                artist.getId(),
                artist.getName(),
                artist.getSlug(),
                artist.getImageUrl()
        );
    }
}
