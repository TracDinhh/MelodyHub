package com.melodyHub.dto.response;

import com.melodyHub.entity.Artist;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistProfileResponse {
    private Integer id;
    private String name;
    private String slug;
    private String bio;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ArtistProfileResponse fromEntity(Artist artist) {
        if (artist == null) {
            return null;
        }

        return new ArtistProfileResponse(
                artist.getId(),
                artist.getName(),
                artist.getSlug(),
                artist.getBio(),
                artist.getImageUrl(),
                artist.getCreatedAt(),
                artist.getUpdatedAt()
        );
    }
}
