package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.LyricsType;
import com.melodyHub.entity.Playlist;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import com.melodyHub.util.SqlSupport;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Plain-JDBC persistence for playlists ({@code playlists}) and their tracks
 * ({@code playlist_songs}). Playlists are owned by a user; all mutating
 * operations are scoped by {@code user_id} so a user can only touch their own.
 */
public class PlaylistRepository {
    private static final int POSITION_GAP = 1000;

    private static final String PLAYLIST_COLUMNS = """
            id, name, description, user_id, cover_url, is_public, created_at, updated_at
            """;

    private static final String SONG_COLUMNS = """
            s.id, s.title, s.slug, s.album_id, s.track_number, s.duration_sec,
            s.file_path, s.cover_url, s.lyrics, s.lyrics_type, s.status, s.play_count,
            s.created_at, s.updated_at, s.deleted_at
            """;

    private final DataSource dataSource;

    public PlaylistRepository() {
        this.dataSource = null;
    }

    public PlaylistRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    // ---- Playlist CRUD --------------------------------------------------

    public Playlist create(Playlist playlist) throws SQLException {
        String sql = """
                INSERT INTO playlists (name, description, user_id, cover_url, is_public)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, playlist.getName());
            statement.setString(2, playlist.getDescription());
            statement.setInt(3, playlist.getUserId());
            statement.setString(4, playlist.getCoverUrl());
            statement.setBoolean(5, playlist.isPublic());
            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating playlist failed, no ID returned.");
                }
                playlist.setId(keys.getInt(1));
            }
        }
        return findByIdForUser(playlist.getUserId(), playlist.getId())
                .orElseThrow(() -> new SQLException("Playlist not found after insert."));
    }

    public List<Playlist> getPageByUser(int userId, int size, int offset) throws SQLException {
        String sql = "SELECT " + PLAYLIST_COLUMNS + """
                 FROM playlists
                 WHERE user_id = ?
                 ORDER BY updated_at DESC, id DESC
                 LIMIT ? OFFSET ?
                """;

        List<Playlist> playlists = new ArrayList<>();
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, size);
            statement.setInt(3, offset);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    playlists.add(mapPlaylistRow(resultSet));
                }
            }
        }
        return playlists;
    }

    public long countByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM playlists WHERE user_id = ?";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    public Optional<Playlist> findByIdForUser(int userId, int playlistId) throws SQLException {
        String sql = "SELECT " + PLAYLIST_COLUMNS + " FROM playlists WHERE id = ? AND user_id = ?";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            statement.setInt(2, userId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapPlaylistRow(resultSet)) : Optional.empty();
            }
        }
    }

    /**
     * Updates a playlist's editable fields. Returns the updated playlist, or
     * empty when the playlist does not exist / is not owned by the user.
     */
    public Optional<Playlist> update(int userId, int playlistId, String name, String description,
            String coverUrl, boolean isPublic) throws SQLException {
        String sql = """
                UPDATE playlists
                SET name = ?, description = ?, cover_url = ?, is_public = ?,
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ? AND user_id = ?
                """;
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, description);
            statement.setString(3, coverUrl);
            statement.setBoolean(4, isPublic);
            statement.setInt(5, playlistId);
            statement.setInt(6, userId);
            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }
        }
        return findByIdForUser(userId, playlistId);
    }

    public boolean delete(int userId, int playlistId) throws SQLException {
        String sql = "DELETE FROM playlists WHERE id = ? AND user_id = ?";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            statement.setInt(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    // ---- Playlist songs -------------------------------------------------

    /**
     * Appends a song to the playlist at the end. Idempotent: if the song is
     * already in the playlist, nothing changes and false is returned. Returns
     * true when a new row was inserted.
     */
    public boolean addSong(int playlistId, int songId) throws SQLException {
        String insert = """
                INSERT INTO playlist_songs (playlist_id, song_id, position)
                SELECT ?, ?, COALESCE(MAX(position), 0) + ?
                FROM playlist_songs
                WHERE playlist_id = ?
                """;
        Connection connection = getConnection();
        try {
            connection.setAutoCommit(false);
            if (containsSong(connection, playlistId, songId)) {
                connection.rollback();
                return false;
            }
            try (var statement = connection.prepareStatement(insert)) {
                statement.setInt(1, playlistId);
                statement.setInt(2, songId);
                statement.setInt(3, POSITION_GAP);
                statement.setInt(4, playlistId);
                statement.executeUpdate();
            }
            touchPlaylist(connection, playlistId);
            connection.commit();
            return true;
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    public boolean removeSong(int playlistId, int songId) throws SQLException {
        String sql = "DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
        Connection connection = getConnection();
        try {
            connection.setAutoCommit(false);
            boolean removed;
            try (var statement = connection.prepareStatement(sql)) {
                statement.setInt(1, playlistId);
                statement.setInt(2, songId);
                removed = statement.executeUpdate() > 0;
            }
            if (removed) {
                touchPlaylist(connection, playlistId);
            }
            connection.commit();
            return removed;
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    /** Returns the published, non-deleted songs of a playlist, in playlist order. */
    public List<Song> getSongs(int playlistId) throws SQLException {
        String sql = "SELECT " + SONG_COLUMNS + """
                 FROM playlist_songs ps
                 JOIN songs s ON s.id = ps.song_id
                 WHERE ps.playlist_id = ?
                   AND s.deleted_at IS NULL
                   AND s.status = ?
                 ORDER BY ps.position ASC, ps.id ASC
                """;

        List<Song> songs = new ArrayList<>();
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            statement.setString(2, SongStatus.PUBLISHED.name());
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    songs.add(mapSongRow(resultSet));
                }
            }
        }
        return songs;
    }

    public int countSongs(int playlistId) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM playlist_songs ps
                JOIN songs s ON s.id = ps.song_id
                WHERE ps.playlist_id = ? AND s.deleted_at IS NULL AND s.status = ?
                """;
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            statement.setString(2, SongStatus.PUBLISHED.name());
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    /**
     * Batched variant of {@link #countSongs(int)}: counts published songs for
     * many playlists in a single grouped query, avoiding the N+1 pattern when
     * listing playlists. Playlist ids with zero songs are absent from the map.
     */
    public java.util.Map<Integer, Integer> countSongsFor(java.util.Collection<Integer> playlistIds)
            throws SQLException {
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        if (playlistIds == null || playlistIds.isEmpty()) {
            return counts;
        }
        List<Integer> ids = playlistIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return counts;
        }

        String placeholders = SqlSupport.placeholders(ids.size());
        String sql = """
                SELECT ps.playlist_id, COUNT(*) AS song_count
                FROM playlist_songs ps
                JOIN songs s ON s.id = ps.song_id
                WHERE ps.playlist_id IN (""" + placeholders + """
                )
                  AND s.deleted_at IS NULL
                  AND s.status = ?
                GROUP BY ps.playlist_id
                """;

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Integer id : ids) {
                statement.setInt(index++, id);
            }
            statement.setString(index, SongStatus.PUBLISHED.name());
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.put(resultSet.getInt("playlist_id"), resultSet.getInt("song_count"));
                }
            }
        }
        return counts;
    }

    // ---- Internals ------------------------------------------------------

    private boolean containsSong(Connection connection, int playlistId, int songId) throws SQLException {
        String sql = "SELECT 1 FROM playlist_songs WHERE playlist_id = ? AND song_id = ? LIMIT 1";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            statement.setInt(2, songId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void touchPlaylist(Connection connection, int playlistId) throws SQLException {
        String sql = "UPDATE playlists SET updated_at = CURRENT_TIMESTAMP(6) WHERE id = ?";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            statement.executeUpdate();
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource == null ? DatabaseConfig.getConnection() : dataSource.getConnection();
    }

    private Playlist mapPlaylistRow(ResultSet resultSet) throws SQLException {
        return new Playlist(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getInt("user_id"),
                resultSet.getString("cover_url"),
                resultSet.getBoolean("is_public"),
                getLocalDateTime(resultSet, "created_at"),
                getLocalDateTime(resultSet, "updated_at")
        );
    }

    private Song mapSongRow(ResultSet resultSet) throws SQLException {
        return new Song(
                resultSet.getInt("id"),
                resultSet.getString("title"),
                resultSet.getString("slug"),
                getNullableInteger(resultSet, "album_id"),
                getNullableShort(resultSet, "track_number"),
                resultSet.getInt("duration_sec"),
                resultSet.getString("file_path"),
                resultSet.getString("cover_url"),
                resultSet.getString("lyrics"),
                LyricsType.fromDatabaseValue(resultSet.getString("lyrics_type")),
                SongStatus.fromDatabaseValue(resultSet.getString("status")),
                resultSet.getLong("play_count"),
                getLocalDateTime(resultSet, "created_at"),
                getLocalDateTime(resultSet, "updated_at"),
                getLocalDateTime(resultSet, "deleted_at")
        );
    }

    private Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        return SqlSupport.getNullableInteger(resultSet, columnName);
    }

    private Short getNullableShort(ResultSet resultSet, String columnName) throws SQLException {
        return SqlSupport.getNullableShort(resultSet, columnName);
    }

    private LocalDateTime getLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
        return SqlSupport.getLocalDateTime(resultSet, columnName);
    }
}
