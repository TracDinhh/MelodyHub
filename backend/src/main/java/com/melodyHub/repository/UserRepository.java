package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.User;
import com.melodyHub.entity.UserRole;
import com.melodyHub.entity.UserStatus;
import com.melodyHub.util.SqlSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private static final String USER_COLUMNS = """
            id,
            username,
            email,
            password_hash,
            display_name,
            avatar_url,
            role,
            status,
            created_at,
            updated_at
            """;

    public User create(User user) throws SQLException {
        String sql = """
                INSERT INTO users (
                    username,
                    email,
                    password_hash,
                    display_name,
                    avatar_url,
                    role,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getDisplayName());
            statement.setString(5, user.getAvatarUrl());
            statement.setString(6, getRoleOrDefault(user).name());
            statement.setString(7, getStatusOrDefault(user).name());

            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating user failed, no ID returned.");
                }

                int id = keys.getInt(1);
                user.setId(id);
                return findById(id).orElse(user);
            }
        }
    }

    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT " + USER_COLUMNS + " FROM users WHERE id = ?";

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT " + USER_COLUMNS + " FROM users WHERE username = ?";

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        }
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT " + USER_COLUMNS + " FROM users WHERE email = ?";

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        }
    }

    public boolean existsByUsername(String username) throws SQLException {
        return findByUsername(username).isPresent();
    }

    public boolean existsByEmail(String email) throws SQLException {
        return findByEmail(email).isPresent();
    }

    public List<User> findPage(UserRole role, String query, int limit, int offset) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + USER_COLUMNS + " FROM users WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (role != null) {
            sql.append(" AND role = ?");
            params.add(role.name());
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (username LIKE ? OR email LIKE ? OR display_name LIKE ?)");
            String like = "%" + query.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (var resultSet = statement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (resultSet.next()) {
                    users.add(mapRow(resultSet));
                }
                return users;
            }
        }
    }

    public long countUsers(UserRole role, String query) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (role != null) {
            sql.append(" AND role = ?");
            params.add(role.name());
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (username LIKE ? OR email LIKE ? OR display_name LIKE ?)");
            String like = "%" + query.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    private void bindParams(java.sql.PreparedStatement statement, List<Object> params) throws SQLException {
        for (int index = 0; index < params.size(); index++) {
            statement.setObject(index + 1, params.get(index));
        }
    }

    public Optional<User> updateRole(int userId, UserRole role) throws SQLException {
        String sql = "UPDATE users SET role = ?, updated_at = CURRENT_TIMESTAMP(6) WHERE id = ?";

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.name());
            statement.setInt(2, userId);

            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }

            return findById(userId);
        }
    }

    public Optional<User> updateProfile(int userId, String displayName, String email, String avatarUrl) throws SQLException {
        String sql = """
                UPDATE users
                SET display_name = ?, email = ?, avatar_url = ?, updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, displayName);
            statement.setString(2, email);
            statement.setString(3, avatarUrl);
            statement.setInt(4, userId);

            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }

            return findById(userId);
        }
    }

    public void updatePassword(int userId, String passwordHash) throws SQLException {
        String sql = """
                UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """;

        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    private User mapRow(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("password_hash"),
                resultSet.getString("display_name"),
                resultSet.getString("avatar_url"),
                UserRole.fromDatabaseValue(resultSet.getString("role")),
                UserStatus.fromDatabaseValue(resultSet.getString("status")),
                getLocalDateTime(resultSet, "created_at"),
                getLocalDateTime(resultSet, "updated_at")
        );
    }

    private UserRole getRoleOrDefault(User user) {
        return user.getRole() == null ? UserRole.USER : user.getRole();
    }

    private UserStatus getStatusOrDefault(User user) {
        return user.getStatus() == null ? UserStatus.ACTIVE : user.getStatus();
    }

    private LocalDateTime getLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
        return SqlSupport.getLocalDateTime(resultSet, columnName);
    }
}
