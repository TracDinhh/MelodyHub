package com.melodyHub.dto.response;

import com.melodyHub.entity.Artist;
import com.melodyHub.entity.ArtistMember;
import com.melodyHub.entity.ArtistMemberRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an artist that the authenticated user is a member of.
 * Returned by {@code GET /api/me/artists}.
 *
 * <p>Contains enough data for the Studio entry view to display the artist
 * selector and redirect to the correct Studio route.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyArtistResponse {
    private Integer artistId;
    private String name;
    private String slug;
    private String imageUrl;
    private ArtistMemberRole memberRole;

    public static MyArtistResponse from(ArtistMember member, Artist artist) {
        return new MyArtistResponse(
                artist.getId(),
                artist.getName(),
                artist.getSlug(),
                artist.getImageUrl(),
                member.getRole()
        );
    }
}
