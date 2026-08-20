package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.Genre;
import com.melodyHub.util.SqlSupport;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

public class GenreRepository {
    private static final String COLUMNS = "id, name, slug";

    private final DataSource dataSource;

    public GenreRepository() {
        this.dataSource = null;
    }

    public GenreRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    /** Returns all genres ordered by name. */
    public List<Genre> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM genres ORDER BY name";
        List<Genre> genres = new ArrayList<>();
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                genres.add(mapRow(resultSet));
            }
        }
        return genres;
    }

    public Optional<Genre> findBySlug(String slug) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM genres WHERE slug = ?";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    /**
     * Returns the genres of a single song ordered by their mapping position
     * (primary genre first).
     */
    public List<Genre> findForSong(int songId) throws SQLException {
        String sql = """
                SELECT g.id, g.name, g.slug
                FROM song_genres sg
                JOIN genres g ON g.id = sg.genre_id
                WHERE sg.song_id = ?
                ORDER BY sg.position
                """;
        List<Genre> genres = new ArrayList<>();
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, songId);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    genres.add(mapRow(resultSet));
                }
            }
        }
        return genres;
    }

    /**
     * Batched variant of {@link #findForSong(int)} that loads the genres for
     * many songs in a single query (avoids N+1 when building a page). Returns a
     * map of {@code songId -> ordered genres}; songs with no genres are absent.
     */
    public Map<Integer, List<Genre>> findForSongs(Collection<Integer> songIds) throws SQLException {
        Map<Integer, List<Genre>> bySong = new LinkedHashMap<>();
        if (songIds == null || songIds.isEmpty()) {
            return bySong;
        }

        List<Integer> ids = songIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return bySong;
        }

        String placeholders = SqlSupport.placeholders(ids.size());
        String sql = """
                SELECT sg.song_id, g.id, g.name, g.slug
                FROM song_genres sg
                JOIN genres g ON g.id = sg.genre_id
                WHERE sg.song_id IN (""" + placeholders + """
                )
                ORDER BY sg.song_id, sg.position
                """;

        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                statement.setInt(i + 1, ids.get(i));
            }
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int songId = resultSet.getInt("song_id");
                    bySong.computeIfAbsent(songId, key -> new ArrayList<>()).add(mapRow(resultSet));
                }
            }
        }
        return bySong;
    }

    /** Returns the number of genres whose id is in {@code genreIds}. */
    public long countByIds(Collection<Integer> genreIds) throws SQLException {
        if (genreIds == null || genreIds.isEmpty()) {
            return 0L;
        }
        List<Integer> ids = genreIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return 0L;
        }

        String placeholders = SqlSupport.placeholders(ids.size());
        String sql = "SELECT COUNT(*) FROM genres WHERE id IN (" + placeholders + ")";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                statement.setInt(i + 1, ids.get(i));
            }
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    private Genre mapRow(ResultSet resultSet) throws SQLException {
        return new Genre(
                resultSet.getShort("id"),
                resultSet.getString("name"),
                resultSet.getString("slug")
        );
    }

    private Connection getConnection() throws SQLException {
        return dataSource == null ? DatabaseConfig.getConnection() : dataSource.getConnection();
    }
}
