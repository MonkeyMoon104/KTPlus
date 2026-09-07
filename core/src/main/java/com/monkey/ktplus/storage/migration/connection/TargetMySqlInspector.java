package com.monkey.ktplus.storage.migration.connection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;
import javax.sql.DataSource;

public final class TargetMySqlInspector {
    public static final String MINIMUM_MYSQL_VERSION = "8.0.0";

    private TargetMySqlInspector() {}

    public static MySqlInspection inspect(DataSource dataSource) throws Exception {
        Objects.requireNonNull(dataSource, "dataSource");
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            String version;
            try (ResultSet resultSet = statement.executeQuery("SELECT VERSION()")) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("MySQL VERSION() returned no rows");
                }
                version = resultSet.getString(1);
            }
            if (version != null && version.toLowerCase(Locale.ROOT).contains("mariadb")) {
                throw new IllegalStateException(
                        "the target responds as MariaDB, not supported by this migrator, verify database.type");
            }
            if (!isAtLeast(version, MINIMUM_MYSQL_VERSION)) {
                throw new IllegalStateException(
                        "MySQL " + MINIMUM_MYSQL_VERSION + "+ required, found: " + version);
            }
            String charset = queryScalar(statement, "SELECT DEFAULT_CHARACTER_SET_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = DATABASE()");
            String collation = queryScalar(statement, "SELECT DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = DATABASE()");
            if (charset == null || !charset.toLowerCase(Locale.ROOT).contains("utf8mb4")) {
                throw new IllegalStateException(
                        "target database charset must be utf8mb4, found: " + charset);
            }
            boolean nonempty = hasNonemptyKtTables(connection);
            return new MySqlInspection(version, charset, collation, nonempty);
        }
    }

    private static boolean hasNonemptyKtTables(Connection connection) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(connection.getCatalog(), null, "kt_%", new String[] {"TABLE"})) {
            while (tables.next()) {
                String table = tables.getString("TABLE_NAME");
                if (table == null) {
                    continue;
                }
                String lower = table.toLowerCase(Locale.ROOT);
                if ("kt_flyway_schema_history".equals(lower) || "kt_schema".equals(lower)) {
                    continue;
                }
                if (!lower.startsWith("kt_")) {
                    continue;
                }
                try (Statement statement = connection.createStatement();
                        ResultSet count = statement.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
                    if (count.next() && count.getLong(1) > 0L) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String queryScalar(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                return null;
            }
            return resultSet.getString(1);
        }
    }

    static boolean isAtLeast(String actualVersion, String minimumVersion) {
        if (actualVersion == null || actualVersion.isBlank()) {
            return false;
        }
        String cleaned = actualVersion.trim();
        int dash = cleaned.indexOf('-');
        if (dash > 0) {
            cleaned = cleaned.substring(0, dash);
        }
        String[] actualParts = cleaned.split("\\.");
        String[] minimumParts = minimumVersion.split("\\.");
        int length = Math.max(actualParts.length, minimumParts.length);
        for (int index = 0; index < length; index++) {
            int actual = index < actualParts.length ? parsePart(actualParts[index]) : 0;
            int minimum = index < minimumParts.length ? parsePart(minimumParts[index]) : 0;
            if (actual != minimum) {
                return actual > minimum;
            }
        }
        return true;
    }

    private static int parsePart(String part) {
        StringBuilder digits = new StringBuilder();
        for (int index = 0; index < part.length(); index++) {
            char character = part.charAt(index);
            if (!Character.isDigit(character)) {
                break;
            }
            digits.append(character);
        }
        if (digits.length() == 0) {
            return 0;
        }
        return Integer.parseInt(digits.toString());
    }

    public record MySqlInspection(String version, String charset, String collation, boolean nonemptyApplicationData) {}
}
