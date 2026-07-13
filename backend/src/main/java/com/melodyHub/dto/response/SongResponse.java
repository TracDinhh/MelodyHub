package com.melodyHub.dto.response;

import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SongResponse {
    private Integer id;
    private String title;
    private String slug;
    private Integer albumId;
    private Short trackNumber;
    private Integer durationSec;
    private String coverUrl;
    private String lyrics;
    private SongStatus status;
    private Long playCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SongResponse fromEntity(Song song) {
        if (song == null) {
            return null;
        }

        return new SongResponse(
                song.getId(),
                song.getTitle(),
                song.getSlug(),
                song.getAlbumId(),
                song.getTrackNumber(),
                song.getDurationSec(),
                song.getCoverUrl(),
                song.getLyrics(),
                song.getStatus(),
                song.getPlayCount(),
                song.getCreatedAt(),
                song.getUpdatedAt()
        );
    }
}
