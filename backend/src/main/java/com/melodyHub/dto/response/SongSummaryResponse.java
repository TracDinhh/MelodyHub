package com.melodyHub.dto.response;

import com.melodyHub.entity.Artist;
import com.melodyHub.entity.Song;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Compact song payload for related-songs lists. Skips lyrics, status,
 * timestamps — only the fields the player + a card need.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SongSummaryResponse {
    private Integer id;
    private String title;
    private String slug;
    private String coverUrl;
    private Integer durationSec;
    private Long playCount;
    private String audioUrl;
    private List<ArtistSummaryResponse> artists;

    public static SongSummaryResponse build(Song song, List<Artist> artists) {
        if (song == null) {
            return null;
        }
        List<ArtistSummaryResponse> artistPayload = artists == null
                ? List.of()
                : artists.stream()
                        .map(ArtistSummaryResponse::fromEntity)
                        .toList();

        return new SongSummaryResponse(
                song.getId(),
                song.getTitle(),
                song.getSlug(),
                song.getCoverUrl(),
                song.getDurationSec(),
                song.getPlayCount(),
                song.getFilePath(),
                artistPayload
        );
    }
}
