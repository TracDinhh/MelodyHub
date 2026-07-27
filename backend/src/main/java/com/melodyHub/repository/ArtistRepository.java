package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.Artist;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    public Artist create(int userId, String name, String slug, String bio, String imageUrl) throws SQLException {
        String sql = """
                INSERT INTO artists (user_id, name, slug, bio, image_url)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setString(2, name);
            statement.setString(3, slug);
            statement.setString(4, bio);
            statement.setString(5, imageUrl);

            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating artist failed, no ID returned.");
                }
                int id = keys.getInt(1);
                return findActiveById(connection, id)
                        .orElseThrow(() -> new SQLException("Artist not found after insert."));
            }
        }
    }

    public boolean existsActiveByUserId(int userId) throws SQLException {
        return findActiveByUserId(userId).isPresent();
    }

    /**
     * Checks whether a slug is already used by ANY artist row (the unique
     * constraint ignores soft-delete), so generated slugs stay collision-free.
     */
    public boolean slugExists(String slug) throws SQLException {
        String sql = "SELECT 1 FROM artists WHERE slug = ? LIMIT 1";
        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public List<AdminRow> findPage(String query, int limit, int offset) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT a.id, a.user_id, a.name, a.slug, a.bio, a.image_url,
                       a.created_at, a.updated_at, a.deleted_at,
                       u.username, u.email
                FROM artists a
                LEFT JOIN users u ON u.id = a.user_id
                WHERE a.deleted_at IS NULL
                """);
        List<Object> params = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            sql.append(" AND (a.name LIKE ? OR a.slug LIKE ?)");
            String like = "%" + query.trim() + "%";
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY a.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql.toString())) {
            for (int index = 0; index < params.size(); index++) {
                statement.setObject(index + 1, params.get(index));
            }
            try (var resultSet = statement.executeQuery()) {
                List<AdminRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new AdminRow(
                            mapRow(resultSet),
                            resultSet.getString("username"),
                            resultSet.getString("email")
                    ));
                }
                return rows;
            }
        }
    }

    public long count(String query) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM artists WHERE deleted_at IS NULL");
        List<Object> params = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            sql.append(" AND (name LIKE ? OR slug LIKE ?)");
            String like = "%" + query.trim() + "%";
            params.add(like);
            params.add(like);
        }

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql.toString())) {
            for (int index = 0; index < params.size(); index++) {
                statement.setObject(index + 1, params.get(index));
            }
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    public record AdminRow(Artist artist, String linkedUsername, String linkedEmail) {
    }

    public Optional<Artist> findActiveBySlug(String slug) throws SQLException {
        String sql = "SELECT " + ARTIST_COLUMNS + """
                 FROM artists
                 WHERE slug = ?
                   AND deleted_at IS NULL
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

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
