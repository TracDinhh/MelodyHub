package com.melodyHub.service.admin;

import com.melodyHub.dto.response.GenreResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.entity.Genre;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import com.melodyHub.entity.UserRole;
import com.melodyHub.exception.AuthException;
import com.melodyHub.exception.SongException;
import com.melodyHub.repository.GenreRepository;
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
    private final GenreRepository genreRepository;

    public AdminSongService() {
        this(new AuthorizationService(), new SongRepository(), new GenreRepository());
    }

    public AdminSongService(AuthorizationService authorizationService, SongRepository songRepository) {
        this(authorizationService, songRepository, new GenreRepository());
    }

    public AdminSongService(AuthorizationService authorizationService, SongRepository songRepository,
                            GenreRepository genreRepository) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.songRepository = Objects.requireNonNull(songRepository);
        this.genreRepository = Objects.requireNonNull(genreRepository);
    }

    /**
     * Lists all songs (any status), with optional filtering by status and title search.
     * Returns songs with their main artist info and genres attached.
     */
    public PagedResponse<SongAdminResponse> listSongs(
            String token,
            SongStatus status,
            String query,
            String sort,
            int page,
            int size
    ) throws AuthException, SQLException {
        authorizationService.requireRole(token, UserRole.ADMIN);

        int offset = Pagination.offset(page, size);
        List<Song> songs = songRepository.findAllPage(status, query, sort, size, offset);
        long total = songRepository.countAll(status, query);

        // Batch-load artists and genres for all songs in one query each.
        List<Integer> songIds = songs.stream().map(Song::getId).toList();
        Map<Integer, List<Artist>> artistMap = songRepository.findArtistsForSongs(songIds);
        Map<Integer, List<Genre>> genreMap = genreRepository.findForSongs(songIds);

        List<SongAdminResponse> items = songs.stream()
                .map(song -> SongAdminResponse.from(
                        song,
                        artistMap.getOrDefault(song.getId(), List.of()),
                        genreMap.getOrDefault(song.getId(), List.of())))
                .toList();

        return new PagedResponse<>(items, total, page, size);
    }

    /**
     * Returns per-status song counts for admin summary badges.
     */
    public Map<String, Long> getStatusCounts(String token) throws AuthException, SQLException {
        authorizationService.requireRole(token, UserRole.ADMIN);
        return songRepository.countAllByStatus();
    }

    /**
     * Admin approval: SUBMITTED → PUBLISHED. The artist can never publish
     * directly; this is the only path that makes a song public.
     */
    public SongAdminResponse approve(String token, int songId) throws AuthException, SQLException, SongException {
        int adminId = authorizationService.requireRole(token, UserRole.ADMIN).getId();

        Song song = songRepository.approveSong(songId, adminId)
                .orElseThrow(() -> new SongException(
                        "INVALID_STATUS_TRANSITION",
                        "Only songs under review (SUBMITTED) can be approved"));
        return toAdminResponse(song);
    }

    /**
     * Admin rejection: SUBMITTED → REJECTED, storing the review reason that the
     * artist sees in Studio.
     */
    public SongAdminResponse reject(String token, int songId, String reviewNote)
            throws AuthException, SQLException, SongException {
        int adminId = authorizationService.requireRole(token, UserRole.ADMIN).getId();

        String note = reviewNote == null || reviewNote.isBlank() ? null : reviewNote.trim();
        if (note == null) {
            throw new SongException("REVIEW_NOTE_REQUIRED", "A rejection reason is required");
        }
        if (note.length() > 500) {
            throw new SongException("REVIEW_NOTE_TOO_LONG", "Rejection reason must be 500 characters or less");
        }

        Song song = songRepository.rejectSong(songId, adminId, note)
                .orElseThrow(() -> new SongException(
                        "INVALID_STATUS_TRANSITION",
                        "Only songs under review (SUBMITTED) can be rejected"));
        return toAdminResponse(song);
    }

    /**
     * Admin status change for non-review transitions. Only PUBLISHED ↔ HIDDEN is
     * allowed here; DRAFT/REJECTED songs must go through the review flow, and a
     * SUBMITTED song through approve/reject.
     */
    public SongAdminResponse updateStatus(String token, int songId, SongStatus newStatus)
            throws AuthException, SQLException, SongException {
        authorizationService.requireRole(token, UserRole.ADMIN);

        Song current = songRepository.findByIdAdmin(songId)
                .orElseThrow(() -> new SongException("SONG_NOT_FOUND", "Song was not found"));

        boolean allowed = (current.getStatus() == SongStatus.PUBLISHED && newStatus == SongStatus.HIDDEN)
                || (current.getStatus() == SongStatus.HIDDEN && newStatus == SongStatus.PUBLISHED);
        if (!allowed) {
            throw new SongException(
                    "INVALID_STATUS_TRANSITION",
                    "Admin can only hide/unhide published songs here; submitted songs use approve/reject");
        }

        int updated = songRepository.updateStatusAdmin(songId, newStatus);
        if (updated == 0) {
            throw new SQLException("Song not found or already deleted");
        }

        Song song = songRepository.findByIdAdmin(songId)
                .orElseThrow(() -> new SQLException("Song not found after update"));
        return toAdminResponse(song);
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

    private SongAdminResponse toAdminResponse(Song song) throws SQLException {
        List<Artist> artists = songRepository.findArtistsForSong(song.getId());
        List<Genre> genres = genreRepository.findForSong(song.getId());
        return SongAdminResponse.from(song, artists, genres);
    }

    /**
     * Admin-specific song response that includes artist + genre info.
     */
    public record SongAdminResponse(
            int id,
            String title,
            String slug,
            Integer durationSec,
            String coverUrl,
            String audioUrl,
            String lyrics,
            String lyricsType,
            String status,
            long playCount,
            String submittedAt,
            String reviewNote,
            String reviewedAt,
            String createdAt,
            String updatedAt,
            List<ArtistBrief> artists,
            List<GenreResponse> genres
    ) {
        public static SongAdminResponse from(Song song, List<Artist> artists, List<Genre> genres) {
            return new SongAdminResponse(
                    song.getId(),
                    song.getTitle(),
                    song.getSlug(),
                    song.getDurationSec(),
                    song.getCoverUrl(),
                    song.getFilePath(),
                    song.getLyrics(),
                    song.getLyricsType() != null ? song.getLyricsType().name() : "PLAIN",
                    song.getStatus() != null ? song.getStatus().name() : "PUBLISHED",
                    song.getPlayCount() != null ? song.getPlayCount() : 0L,
                    song.getSubmittedAt() != null ? song.getSubmittedAt().toString() : null,
                    song.getReviewNote(),
                    song.getReviewedAt() != null ? song.getReviewedAt().toString() : null,
                    song.getCreatedAt() != null ? song.getCreatedAt().toString() : null,
                    song.getUpdatedAt() != null ? song.getUpdatedAt().toString() : null,
                    artists.stream().map(ArtistBrief::from).toList(),
                    genres.stream().map(GenreResponse::fromEntity).toList()
            );
        }
    }

    public record ArtistBrief(int id, String name, String slug, String imageUrl) {
        public static ArtistBrief from(Artist artist) {
            return new ArtistBrief(artist.getId(), artist.getName(), artist.getSlug(), artist.getImageUrl());
        }
    }
}