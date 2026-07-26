package com.melodyHub.dto.request;

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
}
