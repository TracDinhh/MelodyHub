package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public class SongLyricsRepository {
    
    private static final String COLUMNS = "id, song_id, line_number, start_time_ms, lyric_text";
    
    private final DataSource dataSource;
    
    public SongLyricsRepository() {
        this.dataSource = null;
    }
    
    public SongLyricsRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Returns all synced lyric lines for a song, ordered by line_number.
     */
    public List<SyncedLyricLine> findBySongId(int songId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM song_lyrics WHERE song_id = ? ORDER BY line_number";
        
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, songId);
            
            try (var resultSet = statement.executeQuery()) {
                List<SyncedLyricLine> lines = new ArrayList<>();
                while (resultSet.next()) {
                    lines.add(mapRow(resultSet));
                }
                return lines;
            }
        }
    }
    
    /**
     * Deletes all lyric lines for a song and inserts new ones.
     * Used when updating synced lyrics.
     */
    public void replaceForSong(int songId, List<SyncedLyricLine> lines) throws SQLException {
        try (var connection = getConnection()) {
            connection.setAutoCommit(false);
            
            try {
                // Delete existing
                try (var deleteStmt = connection.prepareStatement(
                        "DELETE FROM song_lyrics WHERE song_id = ?")) {
                    deleteStmt.setInt(1, songId);
                    deleteStmt.executeUpdate();
                }
                
                // Insert new
                if (lines != null && !lines.isEmpty()) {
                    String insertSql = "INSERT INTO song_lyrics (song_id, line_number, start_time_ms, lyric_text) VALUES (?, ?, ?, ?)";
                    try (var insertStmt = connection.prepareStatement(insertSql)) {
                        for (int i = 0; i < lines.size(); i++) {
                            SyncedLyricLine line = lines.get(i);
                            insertStmt.setInt(1, songId);
                            insertStmt.setInt(2, i + 1);
                            insertStmt.setInt(3, (int) (line.startTimeMs()));
                            insertStmt.setString(4, line.text());
                            insertStmt.addBatch();
                        }
                        insertStmt.executeBatch();
                    }
                }
                
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
    
    /**
     * Deletes all lyric lines for a song.
     */
    public void deleteBySongId(int songId) throws SQLException {
        String sql = "DELETE FROM song_lyrics WHERE song_id = ?";
        try (var connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, songId);
            statement.executeUpdate();
        }
    }
    
    private Connection getConnection() throws SQLException {
        return dataSource == null ? DatabaseConfig.getConnection() : dataSource.getConnection();
    }
    
    private SyncedLyricLine mapRow(ResultSet resultSet) throws SQLException {
        return new SyncedLyricLine(
                resultSet.getLong("start_time_ms"),
                resultSet.getString("lyric_text")
        );
    }
    
    /**
     * Record for synced lyric line data.
     */
    public record SyncedLyricLine(long startTimeMs, String text) {}
}
