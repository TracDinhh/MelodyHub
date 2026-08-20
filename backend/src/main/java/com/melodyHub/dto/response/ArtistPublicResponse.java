package com.melodyHub.dto.response;

import com.melodyHub.entity.Artist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Public-facing artist info for browsing (home, artist page).
 * {@code followerCount} and {@code following} are only populated by the
 * artist-detail endpoint; list/search results leave them null.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistPublicResponse {
    private Integer id;
    private String name;
    private String slug;
    private String bio;
    private String imageUrl;
    private Long followerCount;
    private Boolean following;

    public static ArtistPublicResponse fromEntity(Artist artist) {
        if (artist == null) {
            return null;
        }

        return new ArtistPublicResponse(
                artist.getId(),
                artist.getName(),
                artist.getSlug(),
                artist.getBio(),
                artist.getImageUrl(),
                null,
                null
        );
    }
}
