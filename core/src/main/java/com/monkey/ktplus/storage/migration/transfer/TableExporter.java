package com.monkey.ktplus.storage.migration.transfer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TableExporter {
    private TableExporter() {}

    public static final class EconomyExport {
        private final TableFingerprint killcoins;
        private final TableFingerprint purchases;

        EconomyExport(TableFingerprint killcoins, TableFingerprint purchases) {
            this.killcoins = killcoins;
            this.purchases = purchases;
        }

        public TableFingerprint killcoins() {
            return killcoins;
        }

        public TableFingerprint purchases() {
            return purchases;
        }
    }

    
    public static EconomyExport exportEconomyPair(
            Connection connection, Path killcoinsTsv, Path purchasesTsv) throws SQLException, IOException {
        Objects.requireNonNull(connection, "connection");
        TableFingerprint killcoins = streamTable(connection, MigrationTableCatalog.KILLCOINS, killcoinsTsv);
        TableFingerprint purchases = streamTable(connection, MigrationTableCatalog.PURCHASES, purchasesTsv);
        return new EconomyExport(killcoins, purchases);
    }

    public static TableFingerprint exportTable(Connection connection, TableSpec spec, Path tsvPath)
            throws SQLException, IOException {
        return streamTable(connection, spec, tsvPath);
    }

    
    public static Map<String, TableFingerprint> exportSnapshot(Connection connection, Path directory)
            throws SQLException, IOException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(directory, "directory");
        Files.createDirectories(directory);

        boolean previousAutoCommit = connection.getAutoCommit();
        int previousIsolation = connection.getTransactionIsolation();
        connection.setAutoCommit(false);
        Map<String, TableFingerprint> fingerprints = new LinkedHashMap<String, TableFingerprint>();
        try {
            try {
                connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException ignored) {
                
            }

            EconomyExport economy = exportEconomyPair(
                    connection,
                    directory.resolve(MigrationTableCatalog.KILLCOINS.name() + ".tsv"),
                    directory.resolve(MigrationTableCatalog.PURCHASES.name() + ".tsv"));
            fingerprints.put(economy.killcoins().tableName(), economy.killcoins());
            fingerprints.put(economy.purchases().tableName(), economy.purchases());

            List<TableSpec> rest = MigrationTableCatalog.forSnapshot();
            for (TableSpec spec : rest) {
                if (spec == MigrationTableCatalog.KILLCOINS || spec == MigrationTableCatalog.PURCHASES) {
                    continue;
                }
                Path out = directory.resolve(spec.name() + ".tsv");
                try {
                    fingerprints.put(spec.name(), exportTable(connection, spec, out));
                } catch (SQLException missing) {
                    Files.writeString(out, headerLine(spec) + "\n", StandardCharsets.UTF_8);
                    fingerprints.put(
                            spec.name(),
                            new TableFingerprint(spec.name(), 0L, TableFingerprinter.emptyChecksum(), null));
                }
            }
            connection.commit();
            return fingerprints;
        } catch (Exception error) {
            connection.rollback();
            if (error instanceof SQLException) {
                throw (SQLException) error;
            }
            if (error instanceof IOException) {
                throw (IOException) error;
            }
            throw new SQLException("snapshot export failed", error);
        } finally {
            connection.setTransactionIsolation(previousIsolation);
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    
    public static Map<String, TableFingerprint> exportForTransfer(
            Connection connection, Path directory, com.monkey.ktplus.storage.migration.MigrationRequest request)
            throws SQLException, IOException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(request, "request");
        Files.createDirectories(directory);

        boolean previousAutoCommit = connection.getAutoCommit();
        int previousIsolation = connection.getTransactionIsolation();
        connection.setAutoCommit(false);
        Map<String, TableFingerprint> fingerprints = new LinkedHashMap<String, TableFingerprint>();
        try {
            try {
                connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException ignored) {
                
            }

            
            EconomyExport economy = exportEconomyPair(
                    connection,
                    directory.resolve(MigrationTableCatalog.KILLCOINS.name() + ".tsv"),
                    directory.resolve(MigrationTableCatalog.PURCHASES.name() + ".tsv"));
            fingerprints.put(economy.killcoins().tableName(), economy.killcoins());
            fingerprints.put(economy.purchases().tableName(), economy.purchases());

            for (TableSpec spec : MigrationTableCatalog.forTransfer(request)) {
                if (spec == MigrationTableCatalog.KILLCOINS || spec == MigrationTableCatalog.PURCHASES) {
                    continue;
                }
                Path out = directory.resolve(spec.name() + ".tsv");
                try {
                    fingerprints.put(spec.name(), exportTable(connection, spec, out));
                } catch (SQLException missing) {
                    Files.writeString(out, headerLine(spec) + "\n", StandardCharsets.UTF_8);
                    fingerprints.put(
                            spec.name(),
                            new TableFingerprint(spec.name(), 0L, TableFingerprinter.emptyChecksum(), null));
                }
            }
            connection.commit();
            return fingerprints;
        } catch (Exception error) {
            connection.rollback();
            if (error instanceof SQLException) {
                throw (SQLException) error;
            }
            if (error instanceof IOException) {
                throw (IOException) error;
            }
            throw new SQLException("transfer export failed", error);
        } finally {
            connection.setTransactionIsolation(previousIsolation);
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private static TableFingerprint streamTable(Connection connection, TableSpec spec, Path path)
            throws SQLException, IOException {
        Files.createDirectories(path.getParent() == null ? path.toAbsolutePath().getParent() : path.getParent());
        MessageDigest digest = sha256();
        long rows = 0L;
        long sumBalance = 0L;
        try (PreparedStatement statement = prepareStreaming(connection, spec.selectSql());
                ResultSet resultSet = statement.executeQuery();
                BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(headerLine(spec));
            writer.newLine();
            while (resultSet.next()) {
                rows++;
                for (int i = 0; i < spec.columns().size(); i++) {
                    if (i > 0) {
                        writer.write('\t');
                        digest.update((byte) 0);
                    }
                    Object value = resultSet.getObject(i + 1);
                    String rendered = TableFingerprinter.stringify(value);
                    writer.write(TsvFormat.escape(value));
                    digest.update(rendered.getBytes(StandardCharsets.UTF_8));
                }
                writer.newLine();
                digest.update((byte) '\n');
                if (spec.includeSumBalance()) {
                    sumBalance += resultSet.getLong("balance");
                }
            }
        }
        return new TableFingerprint(
                spec.name(),
                rows,
                toHex(digest.digest()),
                spec.includeSumBalance() ? Long.valueOf(sumBalance) : null);
    }

    private static PreparedStatement prepareStreaming(Connection connection, String sql) throws SQLException {
        PreparedStatement statement =
                connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        try {
            statement.setFetchSize(500);
        } catch (SQLException ignored) {
            
        }
        return statement;
    }

    private static String headerLine(TableSpec spec) {
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < spec.columns().size(); i++) {
            if (i > 0) {
                header.append('\t');
            }
            header.append(spec.columns().get(i));
        }
        return header.toString();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 not available", error);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", Byte.valueOf(value)));
        }
        return builder.toString();
    }
}
