package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

public class SongRepository {
    private static final String MAIN_ARTIST_ROLE = "MAIN";
    private static final String SONG_COLUMNS = """
            id,
            title,
            slug,
            album_id,
            track_number,
            duration_sec,
            file_path,
            cover_url,
            lyrics,
            status,
            play_count,
            created_at,
            updated_at,
            deleted_at
            """;

    private final DataSource dataSource;

    public SongRepository() {
        this.dataSource = null;
    }

    public SongRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }


    public Song create(Song song, int artistId) throws SQLException {
        String insertSong = """
                INSERT INTO songs (title, slug, duration_sec, file_path, cover_url, lyrics, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        String linkArtist = """
                INSERT INTO song_artists (song_id, artist_id, role, position)
                VALUES (?, ?, 'MAIN', 0)
                """;

        Connection connection = getConnection();
        try {
            connection.setAutoCommit(false);

            int songId;
            try (var statement = connection.prepareStatement(insertSong, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, song.getTitle());
                statement.setString(2, song.getSlug());
                statement.setInt(3, song.getDurationSec() == null ? 0 : song.getDurationSec());
                statement.setString(4, song.getFilePath());
                statement.setString(5, song.getCoverUrl());
                statement.setString(6, song.getLyrics());
                statement.setString(7, (song.getStatus() == null ? SongStatus.PUBLISHED : song.getStatus()).name());
                statement.executeUpdate();

                try (var keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Creating song failed, no ID returned.");
                    }
                    songId = keys.getInt(1);
                }
            }

            try (var statement = connection.prepareStatement(linkArtist)) {
                statement.setInt(1, songId);
                statement.setInt(2, artistId);
                statement.executeUpdate();
            }

            connection.commit();

            return findByIdInternal(connection, songId)
                    .orElseThrow(() -> new SQLException("Song not found after insert."));
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    private Optional<Song> findByIdInternal(Connection connection, int id) throws SQLException {
        String sql = "SELECT " + SONG_COLUMNS + " FROM songs WHERE id = ? AND deleted_at IS NULL";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public List<Song> getPage(int size, int offset, String titleQuery, String genreSlug) throws SQLException {
        List<Object> parameters = new ArrayList<>();
        String sql = "SELECT " + SONG_COLUMNS + " FROM songs"
                + buildFilterClause(titleQuery, genreSlug, parameters)
                + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";
        parameters.add(size);
        parameters.add(offset);

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);

            try (var resultSet = statement.executeQuery()) {
                List<Song> songs = new ArrayList<>();
                while (resultSet.next()) {
                    songs.add(mapRow(resultSet));
                }

                return songs;
            }
        }
    }

    public long count(String titleQuery, String genreSlug) throws SQLException {
        List<Object> parameters = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM songs"
                + buildFilterClause(titleQuery, genreSlug, parameters);

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                return 0L;
            }
        }
    }

    public List<Song> getOwnedPage(int artistId, int size, int offset) throws SQLException {
        String sql = "SELECT " + SONG_COLUMNS + " FROM songs s"
                + ownedByMainArtistClause()
                + " ORDER BY s.created_at DESC, s.id DESC LIMIT ? OFFSET ?";

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            statement.setString(2, MAIN_ARTIST_ROLE);
            statement.setInt(3, size);
            statement.setInt(4, offset);

            try (var resultSet = statement.executeQuery()) {
                List<Song> songs = new ArrayList<>();
                while (resultSet.next()) {
                    songs.add(mapRow(resultSet));
                }

                return songs;
            }
        }
    }

    public List<Song> getPublishedByArtist(int artistId, int size, int offset) throws SQLException {
        String sql = "SELECT " + SONG_COLUMNS + " FROM songs s"
                + publishedByArtistClause()
                + " ORDER BY s.play_count DESC, s.created_at DESC LIMIT ? OFFSET ?";

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            statement.setInt(2, size);
            statement.setInt(3, offset);

            try (var resultSet = statement.executeQuery()) {
                List<Song> songs = new ArrayList<>();
                while (resultSet.next()) {
                    songs.add(mapRow(resultSet));
                }
                return songs;
            }
        }
    }

    public long countPublishedByArtist(int artistId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM songs s" + publishedByArtistClause();

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    private String publishedByArtistClause() {
        return """
                 WHERE s.status = 'PUBLISHED'
                   AND s.deleted_at IS NULL
                   AND EXISTS (
                       SELECT 1 FROM song_artists sa
                       WHERE sa.song_id = s.id AND sa.artist_id = ?
                   )
                """;
    }

    public long countOwned(int artistId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM songs s" + ownedByMainArtistClause();

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            statement.setString(2, MAIN_ARTIST_ROLE);

            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    /**
     * Updates editable fields of a song owned (MAIN) by the artist. Returns the
     * updated song, or empty if the artist does not own it / it does not exist.
     */
    public Optional<Song> updateOwn(int artistId, int songId, String title, String coverUrl, String lyrics)
            throws SQLException {
        String sql = """
                UPDATE songs s
                SET s.title = ?,
                    s.cover_url = ?,
                    s.lyrics = ?,
                    s.updated_at = CURRENT_TIMESTAMP(6)
                WHERE s.id = ?
                  AND s.deleted_at IS NULL
                  AND EXISTS (
                      SELECT 1 FROM song_artists sa
                      WHERE sa.song_id = s.id AND sa.artist_id = ? AND sa.role = ?
                  )
                """;

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, coverUrl);
            statement.setString(3, lyrics);
            statement.setInt(4, songId);
            statement.setInt(5, artistId);
            statement.setString(6, MAIN_ARTIST_ROLE);

            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }
        }

        return findOwnedById(artistId, songId);
    }

    public Optional<Song> findOwnedById(int artistId, int songId) throws SQLException {
        return findOwned(artistId, "s.id = ?", songId);
    }

    public Optional<Song> findOwnedBySlug(int artistId, String slug) throws SQLException {
        return findOwned(artistId, "s.slug = ?", slug);
    }

    private Optional<Song> findOwned(int artistId, String lookupClause, Object lookupValue) throws SQLException {
        String sql = "SELECT " + SONG_COLUMNS + " FROM songs s"
                + ownedByMainArtistClause()
                + " AND " + lookupClause;

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            statement.setString(2, MAIN_ARTIST_ROLE);
            statement.setObject(3, lookupValue);

            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    private String ownedByMainArtistClause() {
        return """
                 WHERE s.deleted_at IS NULL
                   AND EXISTS (
                       SELECT 1
                       FROM song_artists sa
                       WHERE sa.song_id = s.id
                         AND sa.artist_id = ?
                         AND sa.role = ?
                   )
                """;
    }

    private String buildFilterClause(String titleQuery, String genreSlug, List<Object> parameters) {
        StringBuilder clause = new StringBuilder(" WHERE status = ? AND deleted_at IS NULL");
        parameters.add(SongStatus.PUBLISHED.name());

        if (titleQuery != null) {
            clause.append(" AND LOWER(title) LIKE LOWER(?) ESCAPE '!'");
            parameters.add("%" + escapeLike(titleQuery) + "%");
        }

        if (genreSlug != null) {
            clause.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM song_genres sg
                         JOIN genres g ON g.id = sg.genre_id
                         WHERE sg.song_id = songs.id
                           AND g.slug = ?
                     )""");
            parameters.add(genreSlug);
        }

        return clause.toString();
    }

    private String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    public Optional<Song> findBySlug(String slug) throws SQLException {
        String sql = "SELECT " + SONG_COLUMNS + """
                 FROM songs
                 WHERE slug = ?
                   AND status = ?
                   AND deleted_at IS NULL
                """;

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            statement.setString(2, SongStatus.PUBLISHED.name());

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource == null ? DatabaseConfig.getConnection() : dataSource.getConnection();
    }

    private Song mapRow(ResultSet resultSet) throws SQLException {
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
                SongStatus.fromDatabaseValue(resultSet.getString("status")),
                resultSet.getLong("play_count"),
                getLocalDateTime(resultSet, "created_at"),
                getLocalDateTime(resultSet, "updated_at"),
                getLocalDateTime(resultSet, "deleted_at")
        );
    }

    private Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private Short getNullableShort(ResultSet resultSet, String columnName) throws SQLException {
        short value = resultSet.getShort(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private LocalDateTime getLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
