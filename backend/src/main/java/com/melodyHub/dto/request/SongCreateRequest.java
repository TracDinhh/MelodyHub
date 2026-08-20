package com.melodyHub.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SongCreateRequest {
    private String title;
    private String slug;
    private String audioUrl;   // -> songs.file_path
    private String coverUrl;
    private Integer durationSec;
    private String lyrics;
    private String lyricsType; // "PLAIN" or "SYNCED"
    private List<Integer> genreIds; // 1-3 genres chosen from GET /api/genres
}
