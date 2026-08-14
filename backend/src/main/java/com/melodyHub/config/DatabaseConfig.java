package com.melodyHub.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Owns a single shared, pooled {@link javax.sql.DataSource} for the whole app.
 *
 * <p>Previously every {@code getConnection()} call opened a brand-new JDBC
 * connection via {@code DriverManager} — a full TCP + auth handshake per query,
 * which dominates latency and exhausts MySQL {@code max_connections} under load.
 * The pool is built once (lazily, on first use) and connections are borrowed and
 * returned to it.</p>
 */
public final class DatabaseConfig {
    private static final String DB_URL = "db.url";
    private static final String DB_USERNAME = "db.username";
    private static final String DB_PASSWORD = "db.password";
    private static final String DB_DRIVER_CLASS_NAME = "db.driver-class-name";

    private static final String POOL_MAX_SIZE = "db.pool.max-size";
    private static final String POOL_MIN_IDLE = "db.pool.min-idle";
    private static final String POOL_CONNECTION_TIMEOUT_MS = "db.pool.connection-timeout-ms";

    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_IDLE = 2;
    private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 30_000;

    private static volatile HikariDataSource dataSource;

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * The shared connection pool. Repositories that accept a {@code DataSource}
     * can be wired with this so they never fall back to per-query connections.
     */
    public static HikariDataSource getDataSource() {
        HikariDataSource local = dataSource;
        if (local == null) {
            synchronized (DatabaseConfig.class) {
                local = dataSource;
                if (local == null) {
                    local = buildPool();
                    dataSource = local;
                }
            }
        }
        return local;
    }

    /** Closes the pool. Intended for a servlet-context shutdown hook. */
    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private static HikariDataSource buildPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(AppConfig.getRequired(DB_URL));
        config.setUsername(AppConfig.getRequired(DB_USERNAME));
        config.setPassword(AppConfig.getRequired(DB_PASSWORD));
        config.setDriverClassName(AppConfig.getRequired(DB_DRIVER_CLASS_NAME));

        config.setMaximumPoolSize(AppConfig.getInt(POOL_MAX_SIZE, DEFAULT_MAX_POOL_SIZE));
        config.setMinimumIdle(AppConfig.getInt(POOL_MIN_IDLE, DEFAULT_MIN_IDLE));
        config.setConnectionTimeout(
                AppConfig.getInt(POOL_CONNECTION_TIMEOUT_MS, DEFAULT_CONNECTION_TIMEOUT_MS));
        config.setPoolName("melodyhub-pool");

        return new HikariDataSource(config);
    }
}
