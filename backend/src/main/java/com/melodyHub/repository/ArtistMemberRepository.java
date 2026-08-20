package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.ArtistMember;
import com.melodyHub.entity.ArtistMemberRole;
import com.melodyHub.util.SqlSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Plain-JDBC repository for {@link ArtistMember}.
 *
 * <p>Membership is binary: a row exists or it doesn't.
 * There is no status column. To revoke access, call {@link #delete(int, int)}.</p>
 *
 * <p>All authorization checks should use {@link #findByUserAndArtist(int, int)}
 * to look up a specific membership, or {@link #existsByUserAndArtist(int, int)}
 * for a quick boolean check.</p>
 */
public class ArtistMemberRepository {
    private static final String MEMBER_COLUMNS = """
            id,
            artist_id,
            user_id,
            role,
            created_at,
            updated_at
            """;

    /**
     * Returns the membership for a specific (user, artist) pair, if it exists.
     * Used by the authorization layer to verify access and determine role.
     */
    public Optional<ArtistMember> findByUserAndArtist(int userId, int artistId) throws SQLException {
        String sql = "SELECT " + MEMBER_COLUMNS + """
                 FROM artist_members
                 WHERE user_id = ?
                   AND artist_id = ?
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, artistId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    /**
     * Returns true if a membership row exists for this (user, artist) pair.
     * Faster than {@link #findByUserAndArtist} when only a boolean is needed.
     */
    public boolean existsByUserAndArtist(int userId, int artistId) throws SQLException {
        String sql = """
                SELECT 1 FROM artist_members
                WHERE user_id = ? AND artist_id = ?
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
     * Returns all artists a user has membership in, ordered by membership creation time.
     * Used by {@code GET /api/me/artists}.
     */
    public List<ArtistMember> findByUserId(int userId) throws SQLException {
        String sql = "SELECT " + MEMBER_COLUMNS + """
                 FROM artist_members
                 WHERE user_id = ?
                 ORDER BY created_at ASC
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (var resultSet = statement.executeQuery()) {
                List<ArtistMember> members = new ArrayList<>();
                while (resultSet.next()) {
                    members.add(mapRow(resultSet));
                }
                return members;
            }
        }
    }

    /**
     * Returns all members of a given artist profile.
     * Used by the team management screen (OWNER only).
     */
    public List<ArtistMember> findByArtistId(int artistId) throws SQLException {
        String sql = "SELECT " + MEMBER_COLUMNS + """
                 FROM artist_members
                 WHERE artist_id = ?
                 ORDER BY role ASC, created_at ASC
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            try (var resultSet = statement.executeQuery()) {
                List<ArtistMember> members = new ArrayList<>();
                while (resultSet.next()) {
                    members.add(mapRow(resultSet));
                }
                return members;
            }
        }
    }

    /**
     * Creates a new membership row. Throws {@link SQLException} with error code 1062
     * if the (artist_id, user_id) unique constraint is violated (duplicate membership).
     *
     * <p>This method is called inside a DB transaction during Admin approval.</p>
     *
     * @param connection an existing connection (for transaction participation)
     * @param artistId   the artist
     * @param userId     the user to add
     * @param role       OWNER or MANAGER
     * @return the created {@link ArtistMember}
     */
    public ArtistMember create(java.sql.Connection connection, int artistId, int userId, ArtistMemberRole role)
            throws SQLException {
        String sql = """
                INSERT INTO artist_members (artist_id, user_id, role)
                VALUES (?, ?, ?)
                """;

        try (var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, artistId);
            statement.setInt(2, userId);
            statement.setString(3, role.name());
            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating artist_member failed, no ID returned.");
                }
                int id = keys.getInt(1);
                return findById(connection, id)
                        .orElseThrow(() -> new SQLException("artist_member not found after insert."));
            }
        }
    }

    /**
     * Removes a membership (revokes access). The caller must verify that the
     * requesting user is an OWNER before invoking this.
     *
     * @return true if a row was deleted, false if the membership did not exist
     */
    public boolean delete(int userId, int artistId) throws SQLException {
        String sql = """
                DELETE FROM artist_members
                WHERE user_id = ? AND artist_id = ?
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, artistId);
            return statement.executeUpdate() > 0;
        }
    }

    /** Counts distinct artists that have at least one member. Used by admin stats. */
    public long countDistinctArtists() throws SQLException {
        String sql = "SELECT COUNT(DISTINCT artist_id) FROM artist_members";
        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        }
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    private Optional<ArtistMember> findById(java.sql.Connection connection, int id) throws SQLException {
        String sql = "SELECT " + MEMBER_COLUMNS + " FROM artist_members WHERE id = ?";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    private ArtistMember mapRow(ResultSet resultSet) throws SQLException {
        return new ArtistMember(
                resultSet.getInt("id"),
                resultSet.getInt("artist_id"),
                resultSet.getInt("user_id"),
                ArtistMemberRole.fromDatabaseValue(resultSet.getString("role")),
                getLocalDateTime(resultSet, "created_at"),
                getLocalDateTime(resultSet, "updated_at")
        );
    }

    private LocalDateTime getLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
        return SqlSupport.getLocalDateTime(resultSet, columnName);
    }
}
