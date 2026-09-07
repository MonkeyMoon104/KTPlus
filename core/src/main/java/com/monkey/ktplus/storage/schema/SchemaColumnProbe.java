package com.monkey.ktplus.storage.schema;

import com.monkey.ktplus.storage.DatabaseService;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class SchemaColumnProbe {
    private final DatabaseService database;

    public SchemaColumnProbe(DatabaseService database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public boolean hasColumn(String table, String column) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(column, "column");
        String dialect = database.dialect() == null ? "sqlite" : database.dialect().toLowerCase(Locale.ROOT);
        if ("sqlite".equals(dialect)) {
            return sqliteHasColumn(table, column);
        }
        if ("postgresql".equals(dialect) || "postgres".equals(dialect)) {
            return informationSchemaHasColumn(table, column, true);
        }
        return informationSchemaHasColumn(table, column, false);
    }

    private boolean sqliteHasColumn(String table, String column) {
        return database.queryList(
                        "PRAGMA table_info(" + sanitizeIdent(table) + ")",
                        null,
                        resultSet -> resultSet.getString("name"))
                .stream()
                .anyMatch(name -> column.equalsIgnoreCase(name));
    }

    private boolean informationSchemaHasColumn(String table, String column, boolean includePublicSchema) {
        if (includePublicSchema) {
            Optional<Integer> found = database.queryOne(
                    "SELECT COUNT(*) AS c FROM information_schema.columns WHERE table_schema = 'public' "
                            + "AND LOWER(table_name) = LOWER(?) AND LOWER(column_name) = LOWER(?)",
                    statement -> {
                        statement.setString(1, table);
                        statement.setString(2, column);
                    },
                    resultSet -> resultSet.getInt("c"));
            return found.orElse(0) > 0;
        }
        Optional<Integer> found = database.queryOne(
                "SELECT COUNT(*) AS c FROM information_schema.columns "
                        + "WHERE LOWER(table_name) = LOWER(?) AND LOWER(column_name) = LOWER(?)",
                statement -> {
                    statement.setString(1, table);
                    statement.setString(2, column);
                },
                resultSet -> resultSet.getInt("c"));
        return found.orElse(0) > 0;
    }

    private static String sanitizeIdent(String value) {
        if (!value.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("unsafe identifier: " + value);
        }
        return value;
    }
}
