package com.melodyHub.dto.response;

import com.melodyHub.entity.Genre;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenreResponse {
    private Short id;
    private String name;
    private String slug;

    public static GenreResponse fromEntity(Genre genre) {
        if (genre == null) {
            return null;
        }
        return new GenreResponse(genre.getId(), genre.getName(), genre.getSlug());
    }
}