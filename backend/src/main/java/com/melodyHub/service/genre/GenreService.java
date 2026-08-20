package com.melodyHub.service.genre;

import com.melodyHub.dto.response.GenreResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongResponse;
import com.melodyHub.entity.Genre;
import com.melodyHub.entity.Song;
import com.melodyHub.repository.GenreRepository;
import com.melodyHub.repository.SongRepository;
import com.melodyHub.util.Pagination;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class GenreService {
    private final GenreRepository genreRepository;
    private final SongRepository songRepository;

    public GenreService() {
        this(new GenreRepository(), new SongRepository());
    }

    public GenreService(GenreRepository genreRepository, SongRepository songRepository) {
        this.genreRepository = Objects.requireNonNull(genreRepository, "genreRepository must not be null");
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
    }

    /** Lists all genre master data, ordered by name. Public, no auth required. */
    public List<GenreResponse> listAll() throws SQLException {
        return genreRepository.findAll().stream()
                .map(GenreResponse::fromEntity)
                .toList();
    }

    public Optional<GenreResponse> getBySlug(String slug) throws SQLException {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return genreRepository.findBySlug(slug.trim())
                .map(GenreResponse::fromEntity);
    }

    /**
     * Returns a page of PUBLISHED songs belonging to the genre with the given
     * slug. Empty when the genre does not exist. Only published, non-deleted
     * songs are ever exposed (public browse).
     */
    public Optional<PagedResponse<SongResponse>> getPublishedSongsBySlug(String slug, int page, int size)
            throws SQLException {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        if (genreRepository.findBySlug(slug.trim()).isEmpty()) {
            return Optional.empty();
        }

        int offset = Pagination.offset(page, size);
        List<Song> songs = songRepository.getPage(size, offset, null, slug.trim());
        long total = songRepository.count(null, slug.trim());

        // Batch-load genres for the page so each song carries its genres.
        Map<Integer, List<Genre>> genresBySong = genreRepository.findForSongs(
                songs.stream().map(Song::getId).toList());

        List<SongResponse> items = songs.stream()
                .map(song -> {
                    SongResponse response = SongResponse.fromEntity(song);
                    response.setGenres(genresBySong.getOrDefault(song.getId(), List.of())
                            .stream().map(GenreResponse::fromEntity).toList());
                    return response;
                })
                .toList();

        return Optional.of(new PagedResponse<>(items, total, page, size));
    }
}
