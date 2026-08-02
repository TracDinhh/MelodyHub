package com.melodyHub.dto.response;

import com.melodyHub.entity.Playlist;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Full playlist payload including its ordered tracks — used for the playlist
 * detail view.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistDetailResponse {
    private Integer id;
    private String name;
    private String description;
    private String coverUrl;
    private boolean isPublic;
    private int songCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SongSummaryResponse> songs;

    public static PlaylistDetailResponse build(Playlist playlist, List<SongSummaryResponse> songs) {
        if (playlist == null) {
            return null;
        }
        List<SongSummaryResponse> payload = songs == null ? List.of() : songs;
        return new PlaylistDetailResponse(
                playlist.getId(),
                playlist.getName(),
                playlist.getDescription(),
                playlist.getCoverUrl(),
                playlist.isPublic(),
                payload.size(),
                playlist.getCreatedAt(),
                playlist.getUpdatedAt(),
                payload
        );
    }
}
