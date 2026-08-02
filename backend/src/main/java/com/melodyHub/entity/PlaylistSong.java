package com.melodyHub.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistSong {
    private Long id;
    private Integer playlistId;
    private Integer songId;
    private Integer position;
    private LocalDateTime addedAt;
}
