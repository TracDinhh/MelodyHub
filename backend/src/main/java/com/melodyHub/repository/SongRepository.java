package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import java.sql.Connection;
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

    public List<Song> getPage(int size, int offset) throws SQLException {
        String sql = "SELECT " + SONG_COLUMNS + """
                 FROM songs
                 WHERE status = ?
                   AND deleted_at IS NULL
                 ORDER BY created_at DESC, id DESC
                 LIMIT ? OFFSET ?
                """;

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, SongStatus.PUBLISHED.name());
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

    public long count() throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM songs
                WHERE status = ?
                  AND deleted_at IS NULL
                """;

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, SongStatus.PUBLISHED.name());

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                return 0L;
            }
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
