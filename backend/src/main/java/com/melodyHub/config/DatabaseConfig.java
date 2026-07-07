package com.melodyHub.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {
    private static final String DB_URL = "db.url";
    private static final String DB_USERNAME = "db.username";
    private static final String DB_PASSWORD = "db.password";
    private static final String DB_DRIVER_CLASS_NAME = "db.driver-class-name";

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        loadDriver();

        return DriverManager.getConnection(
                AppConfig.getRequired(DB_URL),
                AppConfig.getRequired(DB_USERNAME),
                AppConfig.getRequired(DB_PASSWORD)
        );
    }

    private static void loadDriver() throws SQLException {
        try {
            Class.forName(AppConfig.getRequired(DB_DRIVER_CLASS_NAME));
        } catch (ClassNotFoundException exception) {
            throw new SQLException("Could not load database driver", exception);
        }
    }
}
