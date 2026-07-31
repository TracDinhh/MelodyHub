package com.melodyHub.dto.response;

import com.melodyHub.entity.Artist;
import com.melodyHub.entity.Song;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Listen-history row. Wraps a song plus the user's latest listen timestamp
 * and how long they listened.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListenHistoryResponse {
    private Long id;
    private SongResponse song;
    private LocalDateTime listenedAt;
    private Integer playedSec;
    private List<ArtistSummaryResponse> artists;

    public static ListenHistoryResponse fromEntity(
            Long id,
            Song song,
            LocalDateTime listenedAt,
            Integer playedSec,
            List<Artist> artists) {
        List<ArtistSummaryResponse> artistPayload = artists == null
                ? List.of()
                : artists.stream()
                        .map(ArtistSummaryResponse::fromEntity)
                        .toList();

        return new ListenHistoryResponse(
                id,
                SongResponse.fromEntity(song),
                listenedAt,
                playedSec,
                artistPayload
        );
    }
}
