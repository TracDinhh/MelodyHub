package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.Artist;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public class ArtistRepository {
    private static final String ARTIST_COLUMNS = """
            id,
            user_id,
            name,
            slug,
            bio,
            image_url,
            created_at,
            updated_at,
            deleted_at
            """;

    public Optional<Artist> findActiveByUserId(int userId) throws SQLException {
        String sql = "SELECT " + ARTIST_COLUMNS + """
                 FROM artists
                 WHERE user_id = ?
                   AND deleted_at IS NULL
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        }
    }

    public Optional<Artist> updateProfile(
            int artistId,
            String name,
            String slug,
            String bio,
            String imageUrl
    ) throws SQLException {
        String sql = """
                UPDATE artists
                SET name = ?,
                    slug = ?,
                    bio = ?,
                    image_url = ?,
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                  AND deleted_at IS NULL
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, slug);
            statement.setString(3, bio);
            statement.setString(4, imageUrl);
            statement.setInt(5, artistId);

            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }

            return findActiveById(connection, artistId);
        }
    }

    private Optional<Artist> findActiveById(Connection connection, int artistId) throws SQLException {
        String sql = "SELECT " + ARTIST_COLUMNS + """
                 FROM artists
                 WHERE id = ?
                   AND deleted_at IS NULL
                """;

        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        }
    }

    private Artist mapRow(ResultSet resultSet) throws SQLException {
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

    private Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private LocalDateTime getLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
