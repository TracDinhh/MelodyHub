package com.melodyHub.service.artist;

import com.melodyHub.dto.response.ArtistPublicResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.repository.SongRepository;
import com.melodyHub.util.Pagination;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Public (unauthenticated) browsing of artists and their published songs.
 */
public class PublicArtistService {
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;

    public PublicArtistService() {
        this(new ArtistRepository(), new SongRepository());
    }

    public PublicArtistService(ArtistRepository artistRepository, SongRepository songRepository) {
        this.artistRepository = Objects.requireNonNull(artistRepository, "artistRepository must not be null");
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
    }

    public PagedResponse<ArtistPublicResponse> list(int page, int size, String query) throws SQLException {
        int offset = Pagination.offset(page, size);
        List<ArtistPublicResponse> items = artistRepository.findPage(normalize(query), size, offset)
                .stream()
                .map(row -> ArtistPublicResponse.fromEntity(row.artist()))
                .toList();
        long total = artistRepository.count(normalize(query));
        return new PagedResponse<>(items, total, page, size);
    }

    public Optional<ArtistPublicResponse> getBySlug(String slug) throws SQLException {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return artistRepository.findActiveBySlug(slug.trim()).map(ArtistPublicResponse::fromEntity);
    }

    /**
     * Searches existing active artists by name/slug. Used by the Studio CLAIM
     * flow so a user can find an existing artist profile to request access to.
     */
    public List<ArtistPublicResponse> search(String query) throws SQLException {
        String normalized = normalize(query);
        if (normalized == null) {
            return List.of();
        }
        return artistRepository.findPage(normalized, 20, 0)
                .stream()
                .map(row -> ArtistPublicResponse.fromEntity(row.artist()))
                .toList();
    }

    public Optional<PagedResponse<SongResponse>> getSongsBySlug(String slug, int page, int size)
            throws SQLException {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }

        Optional<Artist> artist = artistRepository.findActiveBySlug(slug.trim());
        if (artist.isEmpty()) {
            return Optional.empty();
        }

        int artistId = artist.get().getId();
        int offset = Pagination.offset(page, size);
        List<SongResponse> items = songRepository.getPublishedByArtist(artistId, size, offset)
                .stream()
                .map(SongResponse::fromEntity)
                .toList();
        long total = songRepository.countPublishedByArtist(artistId);
        return Optional.of(new PagedResponse<>(items, total, page, size));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
