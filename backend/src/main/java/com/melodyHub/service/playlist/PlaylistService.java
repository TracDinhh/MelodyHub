package com.melodyHub.service.playlist;

import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.PlaylistDetailResponse;
import com.melodyHub.dto.response.PlaylistResponse;
import com.melodyHub.dto.response.SongSummaryResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.entity.Playlist;
import com.melodyHub.entity.Song;
import com.melodyHub.repository.PlaylistRepository;
import com.melodyHub.repository.SongRepository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Business logic for a user's playlists: create, list, read (with tracks),
 * update, delete, and add/remove songs. All operations are scoped to the
 * owning user.
 */
public class PlaylistService {
    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final PlaylistRepository playlistRepository;
    private final SongRepository songRepository;

    public PlaylistService() {
        this(new PlaylistRepository(), new SongRepository());
    }

    public PlaylistService(PlaylistRepository playlistRepository, SongRepository songRepository) {
        this.playlistRepository = Objects.requireNonNull(playlistRepository, "playlistRepository must not be null");
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
    }

    public PlaylistResponse create(int userId, String name, String description, String coverUrl, Boolean isPublic)
            throws SQLException {
        String cleanName = requireName(name);
        Playlist playlist = new Playlist(
                null,
                cleanName,
                trimToLength(description, MAX_DESCRIPTION_LENGTH),
                userId,
                blankToNull(coverUrl),
                Boolean.TRUE.equals(isPublic),
                null,
                null
        );
        Playlist created = playlistRepository.create(playlist);
        return PlaylistResponse.fromEntity(created, 0);
    }

    public PagedResponse<PlaylistResponse> getPage(int userId, int page, int size) throws SQLException {
        int offset = offset(page, size);
        List<Playlist> playlists = playlistRepository.getPageByUser(userId, size, offset);
        long total = playlistRepository.countByUser(userId);

        List<PlaylistResponse> items = new ArrayList<>(playlists.size());
        for (Playlist playlist : playlists) {
            int songCount = playlistRepository.countSongs(playlist.getId());
            items.add(PlaylistResponse.fromEntity(playlist, songCount));
        }
        return new PagedResponse<>(items, total, page, size);
    }

    public Optional<PlaylistDetailResponse> getDetail(int userId, int playlistId) throws SQLException {
        Optional<Playlist> playlistOpt = playlistRepository.findByIdForUser(userId, playlistId);
        if (playlistOpt.isEmpty()) {
            return Optional.empty();
        }
        Playlist playlist = playlistOpt.get();
        List<SongSummaryResponse> songs = toSummaries(playlistRepository.getSongs(playlistId));
        return Optional.of(PlaylistDetailResponse.build(playlist, songs));
    }

    public Optional<PlaylistResponse> update(int userId, int playlistId, String name, String description,
            String coverUrl, Boolean isPublic) throws SQLException {
        String cleanName = requireName(name);
        Optional<Playlist> updated = playlistRepository.update(
                userId,
                playlistId,
                cleanName,
                trimToLength(description, MAX_DESCRIPTION_LENGTH),
                blankToNull(coverUrl),
                Boolean.TRUE.equals(isPublic)
        );
        if (updated.isEmpty()) {
            return Optional.empty();
        }
        int songCount = playlistRepository.countSongs(playlistId);
        return Optional.of(PlaylistResponse.fromEntity(updated.get(), songCount));
    }

    public boolean delete(int userId, int playlistId) throws SQLException {
        return playlistRepository.delete(userId, playlistId);
    }

    /**
     * Adds a song to a playlist the user owns. Returns empty when the playlist
     * is not found / not owned. The boolean is true when the song was newly
     * added, false when it was already present.
     */
    public Optional<Boolean> addSong(int userId, int playlistId, Integer songId) throws SQLException {
        if (songId == null || songId < 1) {
            throw new IllegalArgumentException("songId is required");
        }
        if (playlistRepository.findByIdForUser(userId, playlistId).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(playlistRepository.addSong(playlistId, songId));
    }

    /**
     * Removes a song from a playlist the user owns. Returns empty when the
     * playlist is not found / not owned. The boolean is true when a song was
     * removed, false when it was not in the playlist.
     */
    public Optional<Boolean> removeSong(int userId, int playlistId, int songId) throws SQLException {
        if (playlistRepository.findByIdForUser(userId, playlistId).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(playlistRepository.removeSong(playlistId, songId));
    }

    // ---- Helpers --------------------------------------------------------

    private int offset(int page, int size) {
        long offset = (long) (page - 1) * size;
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("page is too large");
        }
        return (int) offset;
    }

    private String requireName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    private String trimToLength(String value, int maxLength) {
        String cleaned = blankToNull(value);
        if (cleaned == null) {
            return null;
        }
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException("value must not exceed " + maxLength + " characters");
        }
        return cleaned;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<SongSummaryResponse> toSummaries(List<Song> songs) throws SQLException {
        List<SongSummaryResponse> items = new ArrayList<>(songs.size());
        for (Song song : songs) {
            List<Artist> artists = songRepository.findArtistsForSong(song.getId());
            items.add(SongSummaryResponse.build(song, artists));
        }
        return items;
    }
}
