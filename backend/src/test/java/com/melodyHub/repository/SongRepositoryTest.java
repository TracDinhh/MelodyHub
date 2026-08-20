package com.melodyHub.repository;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SongRepositoryTest {

    @Test
    void genreFilteredPageBuildsValidSql() {
        SongRepository repository = new SongRepository(sqlCheckingDataSource());

        assertDoesNotThrow(() -> repository.getPage(6, 0, null, "pop"));
    }

    private DataSource sqlCheckingDataSource() {
        ResultSet resultSet = proxy(ResultSet.class, (method, args) -> switch (method.getName()) {
            case "next" -> false;
            default -> defaultValue(method.getReturnType());
        });

        PreparedStatement statement = proxy(PreparedStatement.class, (method, args) -> switch (method.getName()) {
            case "executeQuery" -> resultSet;
            default -> defaultValue(method.getReturnType());
        });

        Connection connection = proxy(Connection.class, (method, args) -> {
            if ("prepareStatement".equals(method.getName())) {
                String sql = (String) args[0];
                if (sql.contains("NULLAND")) {
                    throw new SQLException("Malformed adjacent SQL predicates");
                }
                return statement;
            }
            return defaultValue(method.getReturnType());
        });

        return proxy(DataSource.class, (method, args) -> {
            if ("getConnection".equals(method.getName())) {
                return connection;
            }
            return defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, SqlInvocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> invocation.invoke(method, args)
        );
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }

    @FunctionalInterface
    private interface SqlInvocation {
        Object invoke(Method method, Object[] args) throws Throwable;
    }
}
