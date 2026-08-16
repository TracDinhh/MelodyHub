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
 * A song the user has liked, paired with when they liked it and the song's
 * artists. Mirrors {@link ListenHistoryResponse}'s shape so the frontend can
 * reuse the same track-row rendering.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LikedSongResponse {
    private SongResponse song;
    private LocalDateTime likedAt;
    private List<ArtistSummaryResponse> artists;

    public static LikedSongResponse fromEntity(
            Song song,
            LocalDateTime likedAt,
            List<Artist> artists) {
        List<ArtistSummaryResponse> artistPayload = artists == null
                ? List.of()
                : artists.stream()
                        .map(ArtistSummaryResponse::fromEntity)
                        .toList();

        return new LikedSongResponse(
                SongResponse.fromEntity(song),
                likedAt,
                artistPayload
        );
    }
}
