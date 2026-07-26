package com.melodyHub.dto.response;

import com.melodyHub.entity.Artist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Public-facing artist info for browsing (home, artist page).
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

    public static ArtistPublicResponse fromEntity(Artist artist) {
        if (artist == null) {
            return null;
        }

        return new ArtistPublicResponse(
                artist.getId(),
                artist.getName(),
                artist.getSlug(),
                artist.getBio(),
                artist.getImageUrl()
        );
    }
}
