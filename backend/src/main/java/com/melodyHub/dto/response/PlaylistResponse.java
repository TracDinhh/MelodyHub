package com.melodyHub.dto.response;

import com.melodyHub.entity.Playlist;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Playlist payload without its tracks — used for playlist listings. Includes
 * the track count so a card can show "N songs" without loading every track.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistResponse {
    private Integer id;
    private String name;
    private String description;
    private String coverUrl;
    private boolean isPublic;
    private int songCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PlaylistResponse fromEntity(Playlist playlist, int songCount) {
        if (playlist == null) {
            return null;
        }
        return new PlaylistResponse(
                playlist.getId(),
                playlist.getName(),
                playlist.getDescription(),
                playlist.getCoverUrl(),
                playlist.isPublic(),
                songCount,
                playlist.getCreatedAt(),
                playlist.getUpdatedAt()
        );
    }
}
