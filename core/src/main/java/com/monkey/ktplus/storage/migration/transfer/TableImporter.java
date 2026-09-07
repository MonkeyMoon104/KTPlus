package com.monkey.ktplus.storage.migration.transfer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class TableImporter {
    public static final class ImportCounts {
        private final long inserted;
        private final long skipped;

        public ImportCounts(long inserted, long skipped) {
            this.inserted = inserted;
            this.skipped = skipped;
        }

        public long inserted() {
            return inserted;
        }

        public long skipped() {
            return skipped;
        }
    }

    private TableImporter() {}

    public static ImportCounts importTable(
            Connection connection,
            TableSpec spec,
            Path tsvPath,
            String targetDialect,
            ConflictPolicy policy)
            throws SQLException, IOException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(tsvPath, "tsvPath");
        Objects.requireNonNull(targetDialect, "targetDialect");
        Objects.requireNonNull(policy, "policy");
        if (policy != ConflictPolicy.SKIP_EXISTING) {
            throw new IllegalArgumentException("only SKIP_EXISTING is supported");
        }
        if (!Files.isRegularFile(tsvPath)) {
            return new ImportCounts(0L, 0L);
        }

        long inserted = 0L;
        long skipped = 0L;
        String insertSql = insertSql(connection, spec, targetDialect);
        String existsSql = existsSql(connection, spec);
        try (BufferedReader reader = Files.newBufferedReader(tsvPath, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                return new ImportCounts(0L, 0L);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = TsvFormat.splitLine(line);
                if (values.size() != spec.columns().size()) {
                    throw new IOException(
                            "column count mismatch in " + tsvPath + " for table " + spec.name());
                }
                if (exists(connection, spec, existsSql, values)) {
                    skipped++;
                    continue;
                }
                try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                    for (int i = 0; i < values.size(); i++) {
                        bind(insert, i + 1, spec.columns().get(i), values.get(i));
                    }
                    int updated = insert.executeUpdate();
                    if (updated == 1) {
                        inserted++;
                    } else {
                        skipped++;
                    }
                }
            }
        }
        return new ImportCounts(inserted, skipped);
    }

    private static boolean exists(
            Connection connection, TableSpec spec, String existsSql, List<String> values)
            throws SQLException {
        List<String> pk = spec.primaryKeyColumns();
        try (PreparedStatement statement = connection.prepareStatement(existsSql)) {
            for (int i = 0; i < pk.size(); i++) {
                int columnIndex = spec.columns().indexOf(pk.get(i));
                bind(statement, i + 1, pk.get(i), values.get(columnIndex));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static String existsSql(Connection connection, TableSpec spec) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT 1 FROM ")
                .append(quoteIdentifier(connection, spec.name()))
                .append(" WHERE ");
        List<String> pk = spec.primaryKeyColumns();
        for (int i = 0; i < pk.size(); i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(quoteIdentifier(connection, pk.get(i))).append(" = ?");
        }
        return sql.toString();
    }

    private static String insertSql(Connection connection, TableSpec spec, String targetDialect)
            throws SQLException {
        String dialect = targetDialect.toLowerCase(Locale.ROOT);
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < spec.columns().size(); i++) {
            if (i > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(quoteIdentifier(connection, spec.columns().get(i)));
            placeholders.append('?');
        }
        String table = quoteIdentifier(connection, spec.name());
        if ("sqlite".equals(dialect)) {
            return "INSERT OR IGNORE INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")";
        }
        return "INSERT IGNORE INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")";
    }

    private static String quoteIdentifier(Connection connection, String identifier) throws SQLException {
        try (java.sql.Statement statement = connection.createStatement()) {
            return statement.enquoteIdentifier(identifier, false);
        } catch (AbstractMethodError | UnsupportedOperationException | SQLException ex) {
            String quote = connection.getMetaData().getIdentifierQuoteString();
            if (quote == null || quote.isBlank() || " ".equals(quote)) {
                return identifier;
            }
            return quote + identifier.replace(quote, quote + quote) + quote;
        }
    }

    private static void bind(PreparedStatement statement, int index, String column, String raw)
            throws SQLException {
        if ("\\N".equals(raw)) {
            statement.setObject(index, null);
            return;
        }
        String value = TsvFormat.unescape(raw);
        if (isIntegerColumn(column)) {
            statement.setLong(index, Long.parseLong(value));
            return;
        }
        statement.setString(index, value);
    }

    private static boolean isIntegerColumn(String column) {
        return "balance".equals(column)
                || "x".equals(column)
                || "y".equals(column)
                || "z".equals(column)
                || "created_at".equals(column)
                || "claimed_at".equals(column);
    }
}
