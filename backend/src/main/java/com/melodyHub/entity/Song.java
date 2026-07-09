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
public class Song {
    private Integer id;
    private String title;
    private String slug;
    private Integer albumId;
    private Short trackNumber;
    private Integer durationSec = 0;
    private String filePath;
    private String coverUrl;
    private String lyrics;
    private SongStatus status = SongStatus.PUBLISHED;
    private Long playCount = 0L;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
