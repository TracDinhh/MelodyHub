package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.PasswordResetToken;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class PasswordResetTokenRepository {

    private static final String INSERT_SQL = """
        INSERT INTO password_reset_tokens (user_id, token_hash, expires_at)
        VALUES (?, ?, ?)
        """;

    private static final String FIND_BY_TOKEN_HASH_SQL = """
        SELECT id, user_id, token_hash, expires_at, created_at
        FROM password_reset_tokens
        WHERE token_hash = ? AND used_at IS NULL AND expires_at > ?
        """;

    private static final String MARK_USED_SQL = """
        UPDATE password_reset_tokens SET used_at = ? WHERE id = ?
        """;

    private static final String DELETE_EXPIRED_SQL = """
        DELETE FROM password_reset_tokens WHERE expires_at < ?
        """;

    private static final String DELETE_BY_USER_ID_SQL = """
        DELETE FROM password_reset_tokens WHERE user_id = ?
        """;

    public void create(PasswordResetToken token) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, token.getUserId());
            stmt.setString(2, token.getToken()); // token_hash already hashed
            stmt.setTimestamp(3, Timestamp.valueOf(token.getExpiresAt()));
            stmt.executeUpdate();
        }
    }

    public Optional<PasswordResetToken> findValidByTokenHash(String tokenHash) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_TOKEN_HASH_SQL)) {

            stmt.setString(1, tokenHash);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    PasswordResetToken token = new PasswordResetToken();
                    token.setId(rs.getInt("id"));
                    token.setUserId(rs.getInt("user_id"));
                    token.setToken(rs.getString("token_hash"));
                    token.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
                    token.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return Optional.of(token);
                }
            }
        }
        return Optional.empty();
    }

    public void markUsed(int id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(MARK_USED_SQL)) {

            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public int deleteExpired() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_EXPIRED_SQL)) {

            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            return stmt.executeUpdate();
        }
    }

    public int deleteByUserId(int userId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_BY_USER_ID_SQL)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate();
        }
    }
}
