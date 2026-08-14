package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.Album;
import com.melodyHub.util.SqlSupport;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public class AlbumRepository {
    private static final String ALBUM_COLUMNS = """
            id,
            artist_id,
            title,
            slug,
            album_type,
            cover_url,
            release_date,
            created_at,
            updated_at,
            deleted_at
            """;

    public Optional<Album> findActiveById(int id) throws SQLException {
        String sql = "SELECT " + ALBUM_COLUMNS + """
                 FROM albums
                 WHERE id = ? AND deleted_at IS NULL
                """;
        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    private Album mapRow(ResultSet resultSet) throws SQLException {
        return new Album(
                resultSet.getInt("id"),
                resultSet.getInt("artist_id"),
                resultSet.getString("title"),
                resultSet.getString("slug"),
                resultSet.getString("album_type"),
                resultSet.getString("cover_url"),
                getLocalDate(resultSet, "release_date"),
                getLocalDateTime(resultSet, "created_at"),
                getLocalDateTime(resultSet, "updated_at"),
                getLocalDateTime(resultSet, "deleted_at")
        );
    }

    private LocalDateTime getLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
        return SqlSupport.getLocalDateTime(resultSet, columnName);
    }

    private LocalDateTime getLocalDate(ResultSet resultSet, String columnName) throws SQLException {
        java.sql.Date date = resultSet.getDate(columnName);
        return date == null ? null : date.toLocalDate().atStartOfDay();
    }
}
