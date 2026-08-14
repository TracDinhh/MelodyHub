package com.melodyHub.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Small JDBC helpers shared across repositories: nullable-column reads (which
 * must consult {@link ResultSet#wasNull()} because {@code getInt}/{@code getShort}
 * return 0 for SQL NULL), {@code Timestamp → LocalDateTime} conversion, and
 * {@code IN (...)} placeholder generation for batched queries.
 */
public final class SqlSupport {
    private SqlSupport() {
    }

    public static Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    public static Short getNullableShort(ResultSet resultSet, String columnName) throws SQLException {
        short value = resultSet.getShort(columnName);
        return resultSet.wasNull() ? null : value;
    }

    public static LocalDateTime getLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /** Returns {@code "?,?,?"} with {@code count} placeholders for an SQL {@code IN} clause. */
    public static String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }
}
