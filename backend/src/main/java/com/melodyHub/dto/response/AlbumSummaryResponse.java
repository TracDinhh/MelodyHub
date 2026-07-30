package com.melodyHub.dto.response;

import com.melodyHub.entity.Album;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight album info embedded inside Song responses. Drops artist_id,
 * album_type, release_date — enough to render a chip on the detail page.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlbumSummaryResponse {
    private Integer id;
    private String title;
    private String slug;
    private String coverUrl;

    public static AlbumSummaryResponse fromEntity(Album album) {
        if (album == null) {
            return null;
        }
        return new AlbumSummaryResponse(
                album.getId(),
                album.getTitle(),
                album.getSlug(),
                album.getCoverUrl()
        );
    }
}
