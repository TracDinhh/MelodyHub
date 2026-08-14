package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.ArtistRequest;
import com.melodyHub.entity.ArtistRequestStatus;
import com.melodyHub.util.SqlSupport;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArtistRequestRepository {
    private static final String COLUMNS = """
            id,
            user_id,
            artist_name,
            slug,
            bio,
            image_url,
            status,
            review_note,
            reviewed_by,
            reviewed_at,
            created_at,
            updated_at
            """;

    public ArtistRequest create(int userId, String artistName, String slug, String bio, String imageUrl)
            throws SQLException {
        String sql = """
                INSERT INTO artist_requests (user_id, artist_name, slug, bio, image_url, status)
                VALUES (?, ?, ?, ?, ?, 'PENDING')
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setString(2, artistName);
            statement.setString(3, slug);
            statement.setString(4, bio);
            statement.setString(5, imageUrl);
            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating artist request failed, no ID returned.");
                }
                return findById(connection, keys.getInt(1))
                        .orElseThrow(() -> new SQLException("Artist request not found after insert."));
            }
        }
    }

    public boolean existsPendingByUserId(int userId) throws SQLException {
        String sql = "SELECT 1 FROM artist_requests WHERE user_id = ? AND status = 'PENDING' LIMIT 1";

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public Optional<ArtistRequest> findLatestByUserId(int userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + """
                 FROM artist_requests
                 WHERE user_id = ?
                 ORDER BY created_at DESC
                 LIMIT 1
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<ArtistRequest> findById(int id) throws SQLException {
        try (var connection = DatabaseConfig.getConnection()) {
            return findById(connection, id);
        }
    }

    private Optional<ArtistRequest> findById(Connection connection, int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM artist_requests WHERE id = ?";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public long countByStatus(ArtistRequestStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM artist_requests WHERE status = ?";

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    public List<AdminRow> findPageByStatus(ArtistRequestStatus status, int limit, int offset) throws SQLException {
        String sql = """
                SELECT r.id, r.user_id, r.artist_name, r.slug, r.bio, r.image_url,
                       r.status, r.review_note, r.reviewed_by, r.reviewed_at,
                       r.created_at, r.updated_at,
                       u.username, u.display_name, u.email
                FROM artist_requests r
                JOIN users u ON u.id = r.user_id
                WHERE r.status = ?
                ORDER BY r.created_at ASC
                LIMIT ? OFFSET ?
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, limit);
            statement.setInt(3, offset);

            try (var resultSet = statement.executeQuery()) {
                List<AdminRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new AdminRow(
                            mapRow(resultSet),
                            resultSet.getString("username"),
                            resultSet.getString("display_name"),
                            resultSet.getString("email")
                    ));
                }
                return rows;
            }
        }
    }

    /**
     * Marks a request as reviewed with the given final status. Only transitions a
     * PENDING request; returns empty if the request is missing or already resolved.
     */
    public Optional<ArtistRequest> markReviewed(
            int requestId,
            ArtistRequestStatus status,
            int reviewedByUserId,
            String reviewNote
    ) throws SQLException {
        String sql = """
                UPDATE artist_requests
                SET status = ?,
                    review_note = ?,
                    reviewed_by = ?,
                    reviewed_at = CURRENT_TIMESTAMP(6),
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                  AND status = 'PENDING'
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setString(2, reviewNote);
            statement.setInt(3, reviewedByUserId);
            statement.setInt(4, requestId);

            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }
            return findById(connection, requestId);
        }
    }

    private ArtistRequest mapRow(ResultSet resultSet) throws SQLException {
        return new ArtistRequest(
                resultSet.getInt("id"),
                getNullableInteger(resultSet, "user_id"),
                resultSet.getString("artist_name"),
                resultSet.getString("slug"),
                resultSet.getString("bio"),
                resultSet.getString("image_url"),
                ArtistRequestStatus.fromDatabaseValue(resultSet.getString("status")),
                resultSet.getString("review_note"),
                getNullableInteger(resultSet, "reviewed_by"),
                getLocalDateTime(resultSet, "reviewed_at"),
                getLocalDateTime(resultSet, "created_at"),
                getLocalDateTime(resultSet, "updated_at")
        );
    }

    private Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        return SqlSupport.getNullableInteger(resultSet, columnName);
    }

    private LocalDateTime getLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
        return SqlSupport.getLocalDateTime(resultSet, columnName);
    }

    public record AdminRow(ArtistRequest request, String username, String displayName, String email) {
    }
}
