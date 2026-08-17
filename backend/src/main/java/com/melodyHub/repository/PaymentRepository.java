package com.melodyHub.repository;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.entity.PaymentOrder;
import com.melodyHub.entity.PaymentStatus;
import com.melodyHub.util.SqlSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentRepository {
    private static final String COLUMNS = """
            id, user_id, plan_code, amount, currency, premium_days, transfer_note,
            status, confirmed_by, confirmed_at, created_at
            """;

    public PaymentOrder create(PaymentOrder order) throws SQLException {
        String sql = """
                INSERT INTO payment_orders (user_id, plan_code, amount, currency, premium_days, transfer_note, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (var connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, order.getUserId());
            statement.setString(2, order.getPlanCode());
            statement.setInt(3, order.getAmount());
            statement.setString(4, order.getCurrency());
            statement.setInt(5, order.getPremiumDays());
            statement.setString(6, order.getTransferNote());
            statement.setString(7, order.getStatus().name());
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Creating payment order failed, no ID returned.");
                order.setId(keys.getLong(1));
            }
        }
        return findById(order.getId()).orElseThrow(() -> new SQLException("Payment order not found after insert."));
    }

    public Optional<PaymentOrder> findById(long id) throws SQLException {
        return findOne("SELECT " + COLUMNS + " FROM payment_orders WHERE id = ?", id);
    }

    public Optional<PaymentOrder> findByNote(String note) throws SQLException {
        return findOne("SELECT " + COLUMNS + " FROM payment_orders WHERE transfer_note = ?", note);
    }

    public Optional<PaymentOrder> findPendingByUser(int userId) throws SQLException {
        String sql = "SELECT " + COLUMNS
                + " FROM payment_orders WHERE user_id = ? AND status = 'PENDING'"
                + " ORDER BY created_at DESC, id DESC LIMIT 1";
        return findOne(sql, userId);
    }

    public List<PaymentOrder> findByUser(int userId, int limit, int offset) throws SQLException {
        return findMany("SELECT " + COLUMNS + " FROM payment_orders WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?", userId, limit, offset);
    }

    public long countByUser(int userId) throws SQLException {
        return count("SELECT COUNT(*) FROM payment_orders WHERE user_id = ?", userId);
    }

    public List<PaymentOrder> findPending(int limit, int offset) throws SQLException {
        return findMany("SELECT " + COLUMNS + " FROM payment_orders WHERE status = 'PENDING' ORDER BY created_at ASC, id ASC LIMIT ? OFFSET ?", limit, offset);
    }

    public long countPending() throws SQLException {
        return count("SELECT COUNT(*) FROM payment_orders WHERE status = 'PENDING'");
    }

    public boolean updateTransferNote(long id, String note) throws SQLException {
        return update("UPDATE payment_orders SET transfer_note = ? WHERE id = ?", note, id);
    }

    public boolean updateStatus(long id, PaymentStatus status, Integer confirmedBy, LocalDateTime confirmedAt) throws SQLException {
        String sql = "UPDATE payment_orders SET status = ?, confirmed_by = ?, confirmed_at = ? WHERE id = ? AND status = 'PENDING'";
        try (var connection = DatabaseConfig.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            if (confirmedBy == null) statement.setNull(2, java.sql.Types.INTEGER); else statement.setInt(2, confirmedBy);
            if (confirmedAt == null) statement.setNull(3, java.sql.Types.TIMESTAMP); else statement.setTimestamp(3, java.sql.Timestamp.valueOf(confirmedAt));
            statement.setLong(4, id);
            return statement.executeUpdate() == 1;
        }
    }

    private Optional<PaymentOrder> findOne(String sql, Object value) throws SQLException {
        try (var connection = DatabaseConfig.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    private List<PaymentOrder> findMany(String sql, int... values) throws SQLException {
        List<PaymentOrder> orders = new ArrayList<>();
        try (var connection = DatabaseConfig.getConnection(); var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setInt(index + 1, values[index]);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) orders.add(mapRow(resultSet));
            }
        }
        return orders;
    }

    private long count(String sql, Object... values) throws SQLException {
        try (var connection = DatabaseConfig.getConnection(); var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            try (var resultSet = statement.executeQuery()) { return resultSet.next() ? resultSet.getLong(1) : 0L; }
        }
    }

    private boolean update(String sql, Object... values) throws SQLException {
        try (var connection = DatabaseConfig.getConnection(); var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            return statement.executeUpdate() == 1;
        }
    }

    private PaymentOrder mapRow(ResultSet resultSet) throws SQLException {
        return new PaymentOrder(resultSet.getLong("id"), resultSet.getInt("user_id"), resultSet.getString("plan_code"),
                resultSet.getInt("amount"), resultSet.getString("currency"), resultSet.getInt("premium_days"),
                resultSet.getString("transfer_note"), PaymentStatus.fromDatabaseValue(resultSet.getString("status")),
                (Integer) resultSet.getObject("confirmed_by"), SqlSupport.getLocalDateTime(resultSet, "confirmed_at"),
                SqlSupport.getLocalDateTime(resultSet, "created_at"));
    }
}
