package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public class RefreshTokenRepository {
    public void create(int userId, String tokenHash, LocalDateTime expiresAt) throws SQLException {
        String sql = """
                INSERT INTO refresh_tokens (
                    user_id,
                    token_hash,
                    expires_at
                )
                VALUES (?, ?, ?)
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, tokenHash);
            statement.setTimestamp(3, Timestamp.valueOf(expiresAt));
            statement.executeUpdate();
        }
    }

    public Optional<Integer> findActiveUserIdByTokenHash(String tokenHash, LocalDateTime now) throws SQLException {
        String sql = """
                SELECT user_id
                FROM refresh_tokens
                WHERE token_hash = ?
                  AND revoked_at IS NULL
                  AND expires_at > ?
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            statement.setTimestamp(2, Timestamp.valueOf(now));

            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(resultSet.getInt("user_id")) : Optional.empty();
            }
        }
    }

    public void revokeByTokenHash(String tokenHash, LocalDateTime revokedAt) throws SQLException {
        String sql = """
                UPDATE refresh_tokens
                SET revoked_at = ?
                WHERE token_hash = ?
                  AND revoked_at IS NULL
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(revokedAt));
            statement.setString(2, tokenHash);
            statement.executeUpdate();
        }
    }
}
