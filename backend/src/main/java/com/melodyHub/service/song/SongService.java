package com.melodyHub.service.song;

import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongDetailResponse;
import com.melodyHub.dto.response.SongResponse;
import com.melodyHub.dto.response.SongSummaryResponse;
import com.melodyHub.entity.Album;
import com.melodyHub.entity.Artist;
import com.melodyHub.entity.Song;
import com.melodyHub.repository.AlbumRepository;
import com.melodyHub.repository.SongRepository;
import com.melodyHub.util.Pagination;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SongService {
    private static final int DEFAULT_RELATED_SIZE = 8;
    private static final int MAX_RELATED_SIZE = 12;

    private final SongRepository songRepository;
    private final AlbumRepository albumRepository;

    public SongService() {
        this(new SongRepository(), new AlbumRepository());
    }

    public SongService(SongRepository songRepository, AlbumRepository albumRepository) {
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
        this.albumRepository = Objects.requireNonNull(albumRepository, "albumRepository must not be null");
    }

    public PagedResponse<SongResponse> getPage(int page, int size, String titleQuery, String genreSlug)
            throws SQLException {
        int offset = Pagination.offset(page, size);
        String normalizedTitle = normalize(titleQuery);
        String normalizedGenre = normalize(genreSlug);

        List<SongResponse> items = songRepository.getPage(size, offset, normalizedTitle, normalizedGenre)
                .stream()
                .map(SongResponse::fromEntity)
                .toList();
        long total = songRepository.count(normalizedTitle, normalizedGenre);

        return new PagedResponse<>(items, total, page, size);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Optional<SongResponse> getBySlug(String slug) throws SQLException {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }

        return songRepository.findBySlug(slug.trim())
                .map(SongResponse::fromEntity);
    }

    /**
     * Returns the rich detail payload for a song, including artists, album,
     * like count, and (if a userId is provided) whether that user liked it.
     * Side effect: bumps the song's play_count by 1.
     */
    public Optional<SongDetailResponse> getDetail(String slug, Integer userId) throws SQLException {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }

        Optional<Song> songOpt = songRepository.findBySlug(slug.trim());
        if (songOpt.isEmpty()) {
            return Optional.empty();
        }
        Song song = songOpt.get();

        List<Artist> artists = songRepository.findArtistsForSong(song.getId());
        Optional<Album> album = song.getAlbumId() == null
                ? Optional.empty()
                : songRepository.findAlbumForSong(song.getAlbumId());

        long likeCount = songRepository.countLikes(song.getId());
        boolean isLiked = userId != null && songRepository.isLikedBy(song.getId(), userId);

        songRepository.incrementPlayCount(song.getId());

        SongDetailResponse response = SongDetailResponse.build(song, artists, album.orElse(null), likeCount, isLiked);
        return Optional.of(response);
    }

    /**
     * Returns up to {@code size} songs related to the song with the given slug.
     * Returns empty when the slug does not exist.
     */
    public List<SongSummaryResponse> getRelated(String slug, int size) throws SQLException {
        if (slug == null || slug.isBlank()) {
            return List.of();
        }
        int bounded = Math.max(1, Math.min(size <= 0 ? DEFAULT_RELATED_SIZE : size, MAX_RELATED_SIZE));

        Optional<Song> songOpt = songRepository.findBySlug(slug.trim());
        if (songOpt.isEmpty()) {
            return List.of();
        }
        Song source = songOpt.get();

        List<Song> related = songRepository.findRelated(
                source.getId(),
                source.getAlbumId() == null ? 0 : source.getAlbumId(),
                bounded
        );

        // Batch-load artists for every related song in one query (avoids N+1).
        List<Integer> relatedIds = related.stream().map(Song::getId).toList();
        java.util.Map<Integer, List<Artist>> artistsBySong =
                songRepository.findArtistsForSongs(relatedIds);

        List<SongSummaryResponse> result = new java.util.ArrayList<>(related.size());
        for (Song song : related) {
            List<Artist> relatedArtists = artistsBySong.getOrDefault(song.getId(), List.of());
            result.add(SongSummaryResponse.build(song, relatedArtists));
        }
        return result;
    }
}
