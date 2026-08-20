package com.melodyHub.dto.response;

import com.melodyHub.entity.Artist;
import com.melodyHub.repository.ArtistRepository;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Artist row for the admin artist list.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistAdminResponse {
    private Integer id;
    private String name;
    private String slug;
    private String bio;
    private String imageUrl;
    private LocalDateTime createdAt;

    public static ArtistAdminResponse fromRow(ArtistRepository.AdminRow row) {
        if (row == null) {
            return null;
        }

        Artist artist = row.artist();
        return new ArtistAdminResponse(
                artist.getId(),
                artist.getName(),
                artist.getSlug(),
                artist.getBio(),
                artist.getImageUrl(),
                artist.getCreatedAt()
        );
    }
}
