package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.ArtistAccessRequest;
import com.melodyHub.entity.ArtistAccessRequestStatus;
import com.melodyHub.entity.ArtistAccessRequestType;
import com.melodyHub.entity.ArtistRelationship;
import com.melodyHub.util.SqlSupport;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plain-JDBC repository for {@link ArtistAccessRequest}.
 *
 * <p>This replaces the legacy {@code ArtistRequestRepository}.
 * It supports both CLAIM_ARTIST and CREATE_ARTIST request types.</p>
 *
 * <p>The approve/reject methods that participate in DB transactions accept a
 * {@link Connection} parameter so they share the transaction with the caller.</p>
 */
public class ArtistAccessRequestRepository {

    private static final String COLUMNS = """
            id,
            user_id,
            artist_id,
            request_type,
            requested_artist_name,
            requested_bio,
            requested_image_url,
            relationship,
            website_url,
            social_url,
            message,
            status,
            review_note,
            reviewed_by,
            reviewed_at,
            created_at,
            updated_at
            """;

    // ─── Write operations ────────────────────────────────────────────────────

    /**
     * Inserts a new PENDING request. Called by {@code ArtistAccessRequestService}.
     *
     * @param connection existing connection (may be null; if null a fresh connection is acquired)
     */
    public ArtistAccessRequest create(
            Integer artistId,
            int userId,
            ArtistAccessRequestType requestType,
            String requestedArtistName,
            String requestedBio,
            String requestedImageUrl,
            ArtistRelationship relationship,
            String websiteUrl,
            String socialUrl,
            String message
    ) throws SQLException {
        String sql = """
                INSERT INTO artist_access_requests
                    (user_id, artist_id, request_type,
                     requested_artist_name, requested_bio, requested_image_url,
                     relationship, website_url, social_url, message, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            setNullableInt(statement, 2, artistId);
            statement.setString(3, requestType.name());
            statement.setString(4, requestedArtistName);
            statement.setString(5, requestedBio);
            statement.setString(6, requestedImageUrl);
            statement.setString(7, relationship.name());
            statement.setString(8, websiteUrl);
            statement.setString(9, socialUrl);
            statement.setString(10, message);
            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating artist_access_request failed, no ID returned.");
                }
                return findById(connection, keys.getInt(1))
                        .orElseThrow(() -> new SQLException("artist_access_request not found after insert."));
            }
        }
    }

    /**
     * Atomically marks a PENDING request as APPROVED.
     * Must be called inside a transaction (connection passed in).
     * Returns empty if the request was not in PENDING status (already processed).
     */
    public Optional<ArtistAccessRequest> markApproved(
            Connection connection,
            int requestId,
            int reviewedByUserId
    ) throws SQLException {
        String sql = """
                UPDATE artist_access_requests
                SET status = 'APPROVED',
                    reviewed_by = ?,
                    reviewed_at = CURRENT_TIMESTAMP(6),
                    updated_at  = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                  AND status = 'PENDING'
                """;

        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reviewedByUserId);
            statement.setInt(2, requestId);
            if (statement.executeUpdate() == 0) {
                return Optional.empty(); // already processed or not found
            }
        }
        return findById(connection, requestId);
    }

    /**
     * Atomically marks a PENDING request as REJECTED.
     * Must be called inside a transaction (connection passed in).
     * Returns empty if the request was not in PENDING status.
     */
    public Optional<ArtistAccessRequest> markRejected(
            Connection connection,
            int requestId,
            int reviewedByUserId,
            String reviewNote
    ) throws SQLException {
        String sql = """
                UPDATE artist_access_requests
                SET status = 'REJECTED',
                    review_note = ?,
                    reviewed_by = ?,
                    reviewed_at = CURRENT_TIMESTAMP(6),
                    updated_at  = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                  AND status = 'PENDING'
                """;

        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, reviewNote);
            statement.setInt(2, reviewedByUserId);
            statement.setInt(3, requestId);
            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }
        }
        return findById(connection, requestId);
    }

    // ─── Read operations ─────────────────────────────────────────────────────

    public Optional<ArtistAccessRequest> findById(int id) throws SQLException {
        try (var connection = DatabaseConfig.getConnection()) {
            return findById(connection, id);
        }
    }

    /** Locks the row FOR UPDATE to prevent double-processing in concurrent approvals. */
    public Optional<ArtistAccessRequest> findByIdForUpdate(Connection connection, int id)
            throws SQLException {
        String sql = "SELECT " + COLUMNS + """
                 FROM artist_access_requests
                 WHERE id = ?
                 FOR UPDATE
                """;

        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    /**
     * Returns the most recent access request for a user (any type, any status).
     * Used by the "Request Status" view.
     */
    public List<ArtistAccessRequest> findAllByUserId(int userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + """
                 FROM artist_access_requests
                 WHERE user_id = ?
                 ORDER BY created_at DESC
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (var resultSet = statement.executeQuery()) {
                List<ArtistAccessRequest> requests = new ArrayList<>();
                while (resultSet.next()) {
                    requests.add(mapRow(resultSet));
                }
                return requests;
            }
        }
    }

    /**
     * Checks whether the user has a pending CLAIM_ARTIST request for a specific artist.
     * Used to enforce the "one pending CLAIM per artist per user" invariant.
     */
    public boolean existsPendingClaimByUserAndArtist(int userId, int artistId) throws SQLException {
        String sql = """
                SELECT 1 FROM artist_access_requests
                WHERE user_id = ?
                  AND artist_id = ?
                  AND request_type = 'CLAIM_ARTIST'
                  AND status = 'PENDING'
                LIMIT 1
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, artistId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Returns a paginated list of requests for Admin review, joined with requester info.
     *
     * @param status filter by status (required)
     */
    public List<AdminRow> findPageByStatus(
            ArtistAccessRequestStatus status,
            int limit,
            int offset
    ) throws SQLException {
        String sql = """
                SELECT r.id, r.user_id, r.artist_id, r.request_type,
                       r.requested_artist_name, r.requested_bio, r.requested_image_url,
                       r.relationship, r.website_url, r.social_url, r.message,
                       r.status, r.review_note, r.reviewed_by, r.reviewed_at,
                       r.created_at, r.updated_at,
                       u.username, u.display_name, u.email,
                       a.name AS artist_name, a.slug AS artist_slug
                FROM artist_access_requests r
                JOIN users u ON u.id = r.user_id
                LEFT JOIN artists a ON a.id = r.artist_id
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
                            resultSet.getString("email"),
                            resultSet.getString("artist_name"),
                            resultSet.getString("artist_slug")
                    ));
                }
                return rows;
            }
        }
    }

    public long countByStatus(ArtistAccessRequestStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM artist_access_requests WHERE status = ?";

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    /**
     * Counts requests grouped by status. Used by admin analytics dashboard.
     */
    public Map<String, Long> countByStatusGrouped() throws SQLException {
        String sql = "SELECT status, COUNT(*) AS total FROM artist_access_requests GROUP BY status";
        Map<String, Long> counts = new LinkedHashMap<>();
        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                counts.put(resultSet.getString("status"), resultSet.getLong("total"));
            }
        }
        return counts;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private Optional<ArtistAccessRequest> findById(Connection connection, int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM artist_access_requests WHERE id = ?";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    private ArtistAccessRequest mapRow(ResultSet resultSet) throws SQLException {
        return new ArtistAccessRequest(
                resultSet.getInt("id"),
                resultSet.getInt("user_id"),
                SqlSupport.getNullableInteger(resultSet, "artist_id"),
                ArtistAccessRequestType.fromDatabaseValue(resultSet.getString("request_type")),
                resultSet.getString("requested_artist_name"),
                resultSet.getString("requested_bio"),
                resultSet.getString("requested_image_url"),
                ArtistRelationship.fromDatabaseValue(resultSet.getString("relationship")),
                resultSet.getString("website_url"),
                resultSet.getString("social_url"),
                resultSet.getString("message"),
                ArtistAccessRequestStatus.fromDatabaseValue(resultSet.getString("status")),
                resultSet.getString("review_note"),
                SqlSupport.getNullableInteger(resultSet, "reviewed_by"),
                SqlSupport.getLocalDateTime(resultSet, "reviewed_at"),
                SqlSupport.getLocalDateTime(resultSet, "created_at"),
                SqlSupport.getLocalDateTime(resultSet, "updated_at")
        );
    }

    private void setNullableInt(java.sql.PreparedStatement statement, int paramIndex, Integer value)
            throws SQLException {
        if (value == null) {
            statement.setNull(paramIndex, java.sql.Types.INTEGER);
        } else {
            statement.setInt(paramIndex, value);
        }
    }

    // ─── Records ─────────────────────────────────────────────────────────────

    /** Enriched row for the admin review queue. */
    public record AdminRow(
            ArtistAccessRequest request,
            String username,
            String displayName,
            String email,
            /** Name of the existing artist (CLAIM_ARTIST only; null for CREATE_ARTIST). */
            String existingArtistName,
            /** Slug of the existing artist (CLAIM_ARTIST only; null for CREATE_ARTIST). */
            String existingArtistSlug
    ) {}
}
