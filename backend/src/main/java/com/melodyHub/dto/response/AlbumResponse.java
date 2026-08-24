package com.melodyHub.dto.response;

import com.melodyHub.entity.Album;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlbumResponse {
    private Integer id;
    private String title;
    private String slug;
    private String albumType;
    private String coverUrl;
    private LocalDateTime releaseDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int songCount;

    public static AlbumResponse fromEntity(Album album) {
        return fromEntity(album, 0);
    }

    public static AlbumResponse fromEntity(Album album, int songCount) {
        if (album == null) {
            return null;
        }
        return new AlbumResponse(
                album.getId(),
                album.getTitle(),
                album.getSlug(),
                album.getAlbumType(),
                album.getCoverUrl(),
                album.getReleaseDate(),
                album.getCreatedAt(),
                album.getUpdatedAt(),
                songCount
        );
    }
}
