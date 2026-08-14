package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.ListenHistory;
import com.melodyHub.entity.LyricsType;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import com.melodyHub.util.SqlSupport;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

public class ListenHistoryRepository {
    private static final String SONG_COLUMNS = """
            s.id, s.title, s.slug, s.album_id, s.track_number, s.duration_sec,
            s.file_path, s.cover_url, s.lyrics, s.status, s.play_count,
            s.lyrics_type,
            s.created_at, s.updated_at, s.deleted_at
            """;

    private final DataSource dataSource;

    public ListenHistoryRepository() {
        this.dataSource = null;
    }

    public ListenHistoryRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    public ListenHistory record(ListenHistory entry) throws SQLException {
        String sql = """
                INSERT INTO listen_history (user_id, song_id, played_sec)
                VALUES (?, ?, ?)
                """;

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, entry.getUserId());
            statement.setInt(2, entry.getSongId());
            statement.setInt(3, entry.getPlayedSec() == null ? 0 : entry.getPlayedSec());
            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Recording listen history failed, no ID returned.");
                }
                entry.setId(keys.getLong(1));
            }
            return entry;
        }
    }

    public long countByUser(int userId) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM listen_history lh
                JOIN songs s ON s.id = lh.song_id
                WHERE lh.user_id = ? AND s.deleted_at IS NULL AND s.status = ?
                """;

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, SongStatus.PUBLISHED.name());

            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    /**
     * Returns songs the user has listened to, newest first, paged. Each row is
     * a {@link HistoryRow} pairing a song with its latest listen timestamp and
     * the corresponding listen_history id.
     */
    public List<HistoryRow> getPageByUser(int userId, int size, int offset) throws SQLException {
        String sql = """
                SELECT lh.id AS lh_id, lh.played_sec, lh.listened_at,
                       """ + SONG_COLUMNS + """
                FROM listen_history lh
                JOIN (
                    SELECT song_id, MAX(listened_at) AS latest_at
                    FROM listen_history
                    WHERE user_id = ?
                    GROUP BY song_id
                ) latest ON latest.song_id = lh.song_id AND latest.latest_at = lh.listened_at
                JOIN songs s ON s.id = lh.song_id
                WHERE lh.user_id = ?
                  AND s.deleted_at IS NULL
                  AND s.status = ?
                GROUP BY s.id, lh.id, lh.played_sec, lh.listened_at
                ORDER BY lh.listened_at DESC, lh.id DESC
                LIMIT ? OFFSET ?
                """;

        List<HistoryRow> rows = new ArrayList<>();
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setString(3, SongStatus.PUBLISHED.name());
            statement.setInt(4, size);
            statement.setInt(5, offset);

            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Song song = mapSongRow(resultSet);
                    LocalDateTime listenedAt = getLocalDateTime(resultSet, "listened_at");
                    int playedSec = resultSet.getInt("played_sec");
                    long historyId = resultSet.getLong("lh_id");
                    rows.add(new HistoryRow(historyId, song, listenedAt, playedSec));
                }
            }
        }
        return rows;
    }

    public boolean deleteForUser(int userId, long historyId) throws SQLException {
        String sql = "DELETE FROM listen_history WHERE id = ? AND user_id = ?";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, historyId);
            statement.setInt(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public int clearAllForUser(int userId) throws SQLException {
        String sql = "DELETE FROM listen_history WHERE user_id = ?";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            return statement.executeUpdate();
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource == null ? DatabaseConfig.getConnection() : dataSource.getConnection();
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

    /** Pairs a song with the user's latest listen timestamp and the listen_history id. */
    public record HistoryRow(long historyId, Song song, LocalDateTime listenedAt, int playedSec) {
    }
}
