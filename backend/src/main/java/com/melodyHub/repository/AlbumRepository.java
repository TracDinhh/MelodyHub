package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.Album;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    public Optional<Album> findOwnedById(int artistId, int albumId) throws SQLException {
        String sql = "SELECT " + ALBUM_COLUMNS + """
                 FROM albums
                 WHERE id = ? AND artist_id = ? AND deleted_at IS NULL
                """;
        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, albumId);
            statement.setInt(2, artistId);
            try ( var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public List<Album> findByArtistId(int artistId) throws SQLException {
        String sql = "SELECT " + ALBUM_COLUMNS + """
                 FROM albums
                 WHERE artist_id = ? AND deleted_at IS NULL
                 ORDER BY created_at DESC
                """;
        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            try ( var resultSet = statement.executeQuery()) {
                List<Album> albums = new ArrayList<>();
                while (resultSet.next()) {
                    albums.add(mapRow(resultSet));
                }
                return albums;
            }
        }
    }

    public List<Album> getPageByArtist(int artistId, int limit, int offset) throws SQLException {
        String sql = "SELECT " + ALBUM_COLUMNS + """
                 FROM albums
                 WHERE artist_id = ? AND deleted_at IS NULL
                 ORDER BY created_at DESC
                 LIMIT ? OFFSET ?
                """;
        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            statement.setInt(2, limit);
            statement.setInt(3, offset);
            try ( var resultSet = statement.executeQuery()) {
                List<Album> albums = new ArrayList<>();
                while (resultSet.next()) {
                    albums.add(mapRow(resultSet));
                }
                return albums;
            }
        }
    }

    public long countByArtist(int artistId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM albums WHERE artist_id = ? AND deleted_at IS NULL";
        try ( var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                return 0;
            }
        }
    }

    public Album create(Album album) throws SQLException {
        String sql = """
                INSERT INTO albums (artist_id, title, slug, album_type, cover_url, release_date, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, album.getArtistId());
            statement.setString(2, album.getTitle());
            statement.setString(3, album.getSlug());
            statement.setString(4, album.getAlbumType() != null ? album.getAlbumType() : "ALBUM");
            setNullableString(statement, 5, album.getCoverUrl());
            setNullableLocalDateTime(statement, 6, album.getReleaseDate());
            setNullableLocalDateTime(statement, 7, album.getCreatedAt());
            setNullableLocalDateTime(statement, 8, album.getUpdatedAt());

            statement.executeUpdate();
            try ( var keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    album.setId(keys.getInt(1));
                }
            }
            return album;
        }
    }

    public Album update(Album album) throws SQLException {
        String sql = """
                UPDATE albums
                SET title = ?, slug = ?, album_type = ?, cover_url = ?, release_date = ?, updated_at = ?
                WHERE id = ? AND deleted_at IS NULL
                """;
        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, album.getTitle());
            statement.setString(2, album.getSlug());
            statement.setString(3, album.getAlbumType());
            setNullableString(statement, 4, album.getCoverUrl());
            setNullableLocalDateTime(statement, 5, album.getReleaseDate());
            setNullableLocalDateTime(statement, 6, album.getUpdatedAt());
            statement.setInt(7, album.getId());

            statement.executeUpdate();
            return album;
        }
    }

    public boolean delete(int artistId, int albumId) throws SQLException {
        String sql = """
                UPDATE albums
                SET deleted_at = ?
                WHERE id = ? AND artist_id = ? AND deleted_at IS NULL
                """;
        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, LocalDateTime.now());
            statement.setInt(2, albumId);
            statement.setInt(3, artistId);
            return statement.executeUpdate() > 0;
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
                getLocalDateTime(resultSet, "release_date"),
                getLocalDateTime(resultSet, "created_at"),
                getLocalDateTime(resultSet, "updated_at"),
                getLocalDateTime(resultSet, "deleted_at")
        );
    }

    private LocalDateTime getLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
        var value = resultSet.getObject(columnName);
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        }
        return null;
    }

    private void setNullableString(PreparedStatement statement, int parameterIndex, String value) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, java.sql.Types.VARCHAR);
        } else {
            statement.setString(parameterIndex, value);
        }
    }

    private void setNullableLocalDateTime(PreparedStatement statement, int parameterIndex, LocalDateTime value) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, java.sql.Types.TIMESTAMP);
        } else {
            statement.setObject(parameterIndex, value);
        }
    }
}
