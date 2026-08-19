package com.melodyHub.service.admin;

import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import com.melodyHub.entity.UserRole;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.SongRepository;
import com.melodyHub.service.auth.AuthorizationService;
import com.melodyHub.util.Pagination;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Admin-only management of all songs regardless of ownership.
 */
public class AdminSongService {
    private final AuthorizationService authorizationService;
    private final SongRepository songRepository;

    public AdminSongService() {
        this(new AuthorizationService(), new SongRepository());
    }

    public AdminSongService(AuthorizationService authorizationService, SongRepository songRepository) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.songRepository = Objects.requireNonNull(songRepository);
    }

    /**
     * Lists all songs (any status), with optional filtering by status and title search.
     * Returns songs with their main artist info attached.
     */
    public PagedResponse<SongAdminResponse> listSongs(
            String token,
            SongStatus status,
            String query,
            int page,
            int size
    ) throws AuthException, SQLException {
        authorizationService.requireRole(token, UserRole.ADMIN);

        int offset = Pagination.offset(page, size);
        List<Song> songs = songRepository.findAllPage(status, query, size, offset);
        long total = songRepository.countAll(status, query);

        // Batch-load artists for all songs in one query
        List<Integer> songIds = songs.stream().map(Song::getId).toList();
        Map<Integer, List<Artist>> artistMap = songRepository.findArtistsForSongs(songIds);

        List<SongAdminResponse> items = songs.stream()
                .map(song -> SongAdminResponse.from(song, artistMap.getOrDefault(song.getId(), List.of())))
                .toList();

        return new PagedResponse<>(items, total, page, size);
    }

    /**
     * Changes the status of a song (PUBLISHED, HIDDEN, DRAFT).
     */
    public SongAdminResponse updateStatus(String token, int songId, SongStatus newStatus)
            throws AuthException, SQLException {
        authorizationService.requireRole(token, UserRole.ADMIN);

        int updated = songRepository.updateStatusAdmin(songId, newStatus);
        if (updated == 0) {
            throw new SQLException("Song not found or already deleted");
        }

        Song song = songRepository.findByIdAdmin(songId)
                .orElseThrow(() -> new SQLException("Song not found after update"));
        List<Artist> artists = songRepository.findArtistsForSong(songId);
        return SongAdminResponse.from(song, artists);
    }

    /**
     * Soft-deletes a song (sets deleted_at).
     */
    public void deleteSong(String token, int songId) throws AuthException, SQLException {
        authorizationService.requireRole(token, UserRole.ADMIN);
        int deleted = songRepository.softDeleteAdmin(songId);
        if (deleted == 0) {
            throw new SQLException("Song not found or already deleted");
        }
    }

    /**
     * Admin-specific song response that includes artist info.
     */
    public record SongAdminResponse(
            int id,
            String title,
            String slug,
            Integer durationSec,
            String coverUrl,
            String audioUrl,
            String lyricsType,
            String status,
            long playCount,
            String createdAt,
            String updatedAt,
            List<ArtistBrief> artists
    ) {
        public static SongAdminResponse from(Song song, List<Artist> artists) {
            return new SongAdminResponse(
                    song.getId(),
                    song.getTitle(),
                    song.getSlug(),
                    song.getDurationSec(),
                    song.getCoverUrl(),
                    song.getFilePath(),
                    song.getLyricsType() != null ? song.getLyricsType().name() : "PLAIN",
                    song.getStatus() != null ? song.getStatus().name() : "PUBLISHED",
                    song.getPlayCount() != null ? song.getPlayCount() : 0L,
                    song.getCreatedAt() != null ? song.getCreatedAt().toString() : null,
                    song.getUpdatedAt() != null ? song.getUpdatedAt().toString() : null,
                    artists.stream().map(ArtistBrief::from).toList()
            );
        }
    }

    public record ArtistBrief(int id, String name, String slug, String imageUrl) {
        public static ArtistBrief from(Artist artist) {
            return new ArtistBrief(artist.getId(), artist.getName(), artist.getSlug(), artist.getImageUrl());
        }
    }
}
