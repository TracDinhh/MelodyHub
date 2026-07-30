package com.melodyHub.dto.response;

import com.melodyHub.entity.Album;
import com.melodyHub.entity.Artist;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rich song payload for {@code GET /api/songs/{slug}}. In addition to the
 * fields of {@link SongResponse}, includes the song's artists, album
 * summary, like count, and whether the requesting user has liked it.
 *
 * <p>{@code isLiked} is always {@code false} for anonymous requests — the
 * caller decides what to do with that.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SongDetailResponse {
    private Integer id;
    private String title;
    private String slug;
    private Integer albumId;
    private AlbumSummaryResponse album;
    private Short trackNumber;
    private Integer durationSec;
    private String coverUrl;
    private String audioUrl;
    private String lyrics;
    private SongStatus status;
    private Long playCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ArtistSummaryResponse> artists;
    private Long likeCount;
    private Boolean isLiked;

    public static SongDetailResponse build(
            Song song,
            List<Artist> artists,
            Album album,
            long likeCount,
            boolean isLiked
    ) {
        if (song == null) {
            return null;
        }

        List<ArtistSummaryResponse> artistPayload = artists == null
                ? List.of()
                : artists.stream()
                        .map(ArtistSummaryResponse::fromEntity)
                        .toList();

        return new SongDetailResponse(
                song.getId(),
                song.getTitle(),
                song.getSlug(),
                song.getAlbumId(),
                AlbumSummaryResponse.fromEntity(album),
                song.getTrackNumber(),
                song.getDurationSec(),
                song.getCoverUrl(),
                song.getFilePath(),
                song.getLyrics(),
                song.getStatus(),
                song.getPlayCount(),
                song.getCreatedAt(),
                song.getUpdatedAt(),
                artistPayload,
                likeCount,
                isLiked
        );
    }
}
