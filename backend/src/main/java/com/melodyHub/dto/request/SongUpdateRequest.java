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
public class SongUpdateRequest {
    private String title;
    private String coverUrl;
    private String lyrics;
    private String lyricsType;
    private List<Integer> genreIds; // 1-3 genres chosen from GET /api/genres
}
