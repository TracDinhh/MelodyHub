package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.Artist;
import com.melodyHub.util.SqlSupport;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * JDBC access to the {@code artist_follows} junction table. A follow is a
 * unique (user_id, artist_id) pair; the composite primary key plus
 * {@code INSERT IGNORE} make every operation idempotent.
 */
public class ArtistFollowRepository {
    private static final String FOLLOWED_ARTIST_COLUMNS = """
            a.id,
            a.name,
            a.slug,
            a.bio,
            a.image_url,
            a.created_at,
            a.updated_at,
            a.deleted_at
            """;

    private final DataSource dataSource;

    public ArtistFollowRepository() {
        this.dataSource = null;
    }

    public ArtistFollowRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Records a follow. Idempotent: a duplicate (user, artist) pair is ignored
     * via {@code INSERT IGNORE}. Returns true when a new row was inserted.
     */
    public boolean follow(int artistId, int userId) throws SQLException {
        String sql = "INSERT IGNORE INTO artist_follows (user_id, artist_id) VALUES (?, ?)";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, artistId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Removes a follow. Returns true when a row was deleted, false when the
     * user was not following the artist.
     */
    public boolean unfollow(int artistId, int userId) throws SQLException {
        String sql = "DELETE FROM artist_follows WHERE user_id = ? AND artist_id = ?";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, artistId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean isFollowing(int artistId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM artist_follows WHERE artist_id = ? AND user_id = ? LIMIT 1";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            statement.setInt(2, userId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public long countFollowers(int artistId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM artist_follows WHERE artist_id = ?";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    /**
     * Follower counts for many artists in a single query — used to enrich
     * pages of artists without an N+1. Returns a map keyed by artist id.
     */
    public Map<Integer, Long> countFollowersForArtists(Collection<Integer> artistIds) throws SQLException {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        List<Integer> ids = artistIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return counts;
        }

        String sql = "SELECT artist_id, COUNT(*) AS follower_count FROM artist_follows"
                + " WHERE artist_id IN (" + SqlSupport.placeholders(ids.size()) + ")"
                + " GROUP BY artist_id";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < ids.size(); index++) {
                statement.setInt(index + 1, ids.get(index));
            }
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.put(resultSet.getInt("artist_id"), resultSet.getLong("follower_count"));
                }
            }
        }
        return counts;
    }

    /**
     * Ids of every artist the user follows (soft-deleted artists excluded).
     * Used by the frontend to hydrate the local followed-state set on load.
     */
    public List<Integer> findFollowingArtistIds(int userId) throws SQLException {
        String sql = """
                SELECT af.artist_id
                FROM artist_follows af
                JOIN artists a ON a.id = af.artist_id
                WHERE af.user_id = ? AND a.deleted_at IS NULL
                ORDER BY af.created_at DESC
                """;
        List<Integer> ids = new ArrayList<>();
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("artist_id"));
                }
            }
        }
        return ids;
    }

    public long countFollowing(int userId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM artist_follows af
                JOIN artists a ON a.id = af.artist_id
                WHERE af.user_id = ? AND a.deleted_at IS NULL
                """;
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    /**
     * Returns a page of artists the user follows, most-recently-followed first.
     */
    public List<Artist> findFollowingPage(int userId, int size, int offset) throws SQLException {
        String sql = "SELECT " + FOLLOWED_ARTIST_COLUMNS + """
                 FROM artist_follows af
                 JOIN artists a ON a.id = af.artist_id
                 WHERE af.user_id = ? AND a.deleted_at IS NULL
                 ORDER BY af.created_at DESC
                 LIMIT ? OFFSET ?
                 """;
        List<Artist> artists = new ArrayList<>();
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, size);
            statement.setInt(3, offset);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    artists.add(mapRow(resultSet));
                }
            }
        }
        return artists;
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

    private Connection getConnection() throws SQLException {
        return dataSource == null ? DatabaseConfig.getConnection() : dataSource.getConnection();
    }
}