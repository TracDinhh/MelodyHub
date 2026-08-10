package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.Album;
import com.melodyHub.entity.Artist;
import com.melodyHub.entity.LyricsType;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
            lyrics_type,
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
                INSERT INTO songs (title, slug, duration_sec, file_path, cover_url, lyrics, lyrics_type, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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
                statement.setString(7, (song.getLyricsType() == null ? LyricsType.PLAIN : song.getLyricsType()).name());
                statement.setString(8, (song.getStatus() == null ? SongStatus.PUBLISHED : song.getStatus()).name());
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
    public Optional<Song> updateOwn(int artistId, int songId, String title, String coverUrl, String lyrics, String lyricsType)
            throws SQLException {
        String sql = """
                UPDATE songs s
                SET s.title = ?,
                    s.cover_url = ?,
                    s.lyrics = ?,
                    s.lyrics_type = ?,
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
            statement.setString(4, lyricsType);
            statement.setInt(5, songId);
            statement.setInt(6, artistId);
            statement.setString(7, MAIN_ARTIST_ROLE);

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

    /**
     * Returns all non-deleted artists linked to the song via {@code song_artists},
     * ordered MAIN first, then FEATURED, then by position.
     */
    public List<Artist> findArtistsForSong(int songId) throws SQLException {
        String sql = """
                SELECT a.id, a.user_id, a.name, a.slug, a.bio, a.image_url,
                       a.created_at, a.updated_at, a.deleted_at
                FROM song_artists sa
                JOIN artists a ON a.id = sa.artist_id
                WHERE sa.song_id = ?
                  AND a.deleted_at IS NULL
                ORDER BY FIELD(sa.role, 'MAIN', 'FEATURED'), sa.position
                """;

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, songId);
            try (var resultSet = statement.executeQuery()) {
                List<Artist> artists = new ArrayList<>();
                while (resultSet.next()) {
                    artists.add(mapArtistRow(resultSet));
                }
                return artists;
            }
        }
    }

    /**
     * Returns the active album for the song, if any. The album must not be
     * soft-deleted. Returns empty when the song has no album or the album
     * was deleted.
     */
    public Optional<Album> findAlbumForSong(Integer albumId) throws SQLException {
        if (albumId == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, artist_id, title, slug, album_type, cover_url,
                       release_date, created_at, updated_at, deleted_at
                FROM albums
                WHERE id = ? AND deleted_at IS NULL
                """;
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, albumId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAlbumRow(resultSet));
            }
        }
    }

    public long countLikes(int songId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM song_likes WHERE song_id = ?";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, songId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    public boolean isLikedBy(int songId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM song_likes WHERE song_id = ? AND user_id = ? LIMIT 1";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, songId);
            statement.setInt(2, userId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void incrementPlayCount(int songId) throws SQLException {
        String sql = """
                UPDATE songs
                SET play_count = play_count + 1,
                    updated_at = updated_at
                WHERE id = ? AND deleted_at IS NULL
                """;
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, songId);
            statement.executeUpdate();
        }
    }

    /**
     * Returns up to {@code limit} published songs related to {@code songId}.
     * Priority order: same MAIN artist → same album → anything else.
     * Dedupes by song id. Always excludes the source song itself.
     */
    public List<Song> findRelated(int songId, int albumId, int limit) throws SQLException {
        if (limit <= 0) {
            return List.of();
        }

        String sql = """
                SELECT s.id, s.title, s.slug, s.album_id, s.track_number, s.duration_sec,
                       s.file_path, s.cover_url, s.lyrics, s.status, s.play_count,
                       s.created_at, s.updated_at, s.deleted_at,
                       (
                           SELECT MIN(
                               CASE sa.role
                                   WHEN 'MAIN' THEN 1
                                   WHEN 'FEATURED' THEN 2
                                   ELSE 3
                               END
                           )
                           FROM song_artists sa
                           WHERE sa.song_id = s.id
                             AND sa.artist_id IN (
                                 SELECT sa2.artist_id FROM song_artists sa2 WHERE sa2.song_id = ?
                             )
                       ) AS same_artist_rank,
                       (CASE WHEN s.album_id = ? THEN 0 ELSE 1 END) AS same_album_rank
                FROM songs s
                WHERE s.status = 'PUBLISHED'
                  AND s.deleted_at IS NULL
                  AND s.id <> ?
                ORDER BY same_artist_rank ASC, same_album_rank ASC, s.play_count DESC, s.created_at DESC
                LIMIT ?
                """;

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, songId);
            statement.setInt(2, albumId);
            statement.setInt(3, songId);
            statement.setInt(4, limit);

            try (var resultSet = statement.executeQuery()) {
                List<Song> songs = new ArrayList<>();
                Set<Integer> seen = new HashSet<>();
                while (resultSet.next()) {
                    Song song = mapRow(resultSet);
                    if (seen.add(song.getId())) {
                        songs.add(song);
                    }
                }
                return songs;
            }
        }
    }

    private Artist mapArtistRow(ResultSet resultSet) throws SQLException {
        return new Artist(
                resultSet.getInt("id"),
                getNullableInteger(resultSet, "user_id"),
                resultSet.getString("name"),
                resultSet.getString("slug"),
                resultSet.getString("bio"),
                resultSet.getString("image_url"),
                getLocalDateTime(resultSet, "created_at"),
                getLocalDateTime(resultSet, "updated_at"),
                getLocalDateTime(resultSet, "deleted_at")
        );
    }

    private Album mapAlbumRow(ResultSet resultSet) throws SQLException {
        java.sql.Date releaseDate = resultSet.getDate("release_date");
        LocalDateTime releaseDateTime = releaseDate == null ? null : releaseDate.toLocalDate().atStartOfDay();
        return new Album(
                resultSet.getInt("id"),
                resultSet.getInt("artist_id"),
                resultSet.getString("title"),
                resultSet.getString("slug"),
                resultSet.getString("album_type"),
                resultSet.getString("cover_url"),
                releaseDateTime,
                getLocalDateTime(resultSet, "created_at"),
                getLocalDateTime(resultSet, "updated_at"),
                getLocalDateTime(resultSet, "deleted_at")
        );
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
                LyricsType.fromDatabaseValue(resultSet.getString("lyrics_type")),
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
