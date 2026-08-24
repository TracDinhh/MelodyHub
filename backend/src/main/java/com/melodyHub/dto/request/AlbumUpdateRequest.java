package com.melodyHub.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlbumUpdateRequest {
    private String title;
    private String albumType;
    private String coverUrl;
    private LocalDate releaseDate;
}
