package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.Artist;
import com.melodyHub.util.SqlSupport;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArtistRepository {
    private static final String ARTIST_COLUMNS = """
            id,
            name,
            slug,
            bio,
            image_url,
            created_at,
            updated_at,
            deleted_at
            """;

    /**
     * Checks whether an artist with the given id exists and has not been soft-deleted.
     * Used by {@code ArtistAccessRequestService} to validate CLAIM_ARTIST requests.
     */
    public boolean existsActiveById(int id) throws SQLException {
        String sql = "SELECT 1 FROM artists WHERE id = ? AND deleted_at IS NULL LIMIT 1";
        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
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

    /**
     * Creates a new artist profile without a {@code user_id} link.
     * Used inside a DB transaction during CREATE_ARTIST approval.
     * The caller is responsible for subsequently creating the {@code artist_members} row.
     *
     * @param connection a connection with an active transaction (not closed by this method)
     */
    public Artist createWithConnection(Connection connection, String name, String slug, String bio, String imageUrl)
            throws SQLException {
        String sql = """
                INSERT INTO artists (name, slug, bio, image_url)
                VALUES (?, ?, ?, ?)
                """;

        try (var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, slug);
            statement.setString(3, bio);
            statement.setString(4, imageUrl);
            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating artist failed, no ID returned.");
                }
                return findActiveById(connection, keys.getInt(1))
                        .orElseThrow(() -> new SQLException("Artist not found after insert."));
            }
        }
    }

    /**
     * Looks up an active (non-deleted) artist by ID. Convenience overload for
     * use outside of transaction contexts.
     */
    public Optional<Artist> findActiveById(int id) throws SQLException {
        try (var connection = DatabaseConfig.getConnection()) {
            return findActiveById(connection, id);
        }
    }


    public List<AdminRow> findPage(String query, int limit, int offset) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT a.id, a.name, a.slug, a.bio, a.image_url,
                       a.created_at, a.updated_at, a.deleted_at
                FROM artists a
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
                    rows.add(new AdminRow(mapRow(resultSet)));
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

    public record AdminRow(Artist artist) {
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
                resultSet.getString("name"),
                resultSet.getString("slug"),
                resultSet.getString("bio"),
                resultSet.getString("image_url"),
                getLocalDateTime(resultSet, "created_at"),
                getLocalDateTime(resultSet, "updated_at"),
                getLocalDateTime(resultSet, "deleted_at")
        );
    }

    private LocalDateTime getLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
        return SqlSupport.getLocalDateTime(resultSet, columnName);
    }
}
