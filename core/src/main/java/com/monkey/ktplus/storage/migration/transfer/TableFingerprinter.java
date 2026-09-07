package com.monkey.ktplus.storage.migration.transfer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TableFingerprinter {
    private TableFingerprinter() {}

    public static TableFingerprint fingerprint(Connection connection, TableSpec spec) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(spec, "spec");
        MessageDigest digest = sha256();
        long rows = 0L;
        Long sumBalance = spec.includeSumBalance() ? Long.valueOf(0L) : null;
        try (PreparedStatement statement = connection.prepareStatement(spec.selectSql());
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rows++;
                for (int i = 0; i < spec.columns().size(); i++) {
                    if (i > 0) {
                        digest.update((byte) 0);
                    }
                    Object value = resultSet.getObject(i + 1);
                    digest.update(stringify(value).getBytes(StandardCharsets.UTF_8));
                }
                digest.update((byte) '\n');
                if (sumBalance != null) {
                    sumBalance = Long.valueOf(sumBalance.longValue() + resultSet.getLong("balance"));
                }
            }
        }
        return new TableFingerprint(spec.name(), rows, toHex(digest.digest()), sumBalance);
    }

    
    public static Map<String, TableFingerprint> fingerprintEconomyPair(Connection connection)
            throws SQLException {
        Map<String, TableFingerprint> out = new LinkedHashMap<String, TableFingerprint>();
        out.put(MigrationTableCatalog.KILLCOINS.name(), fingerprint(connection, MigrationTableCatalog.KILLCOINS));
        out.put(MigrationTableCatalog.PURCHASES.name(), fingerprint(connection, MigrationTableCatalog.PURCHASES));
        return out;
    }

    public static Map<String, TableFingerprint> fingerprintAllInOneTransaction(Connection connection)
            throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        int previousIsolation = connection.getTransactionIsolation();
        connection.setAutoCommit(false);
        try {
            try {
                connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException ignored) {
                
            }
            Map<String, TableFingerprint> out = new LinkedHashMap<String, TableFingerprint>();
            for (TableSpec spec : MigrationTableCatalog.forSnapshot()) {
                try {
                    out.put(spec.name(), fingerprint(connection, spec));
                } catch (SQLException missingTable) {
                    out.put(spec.name(), new TableFingerprint(spec.name(), 0L, emptyChecksum(), null));
                }
            }
            connection.commit();
            return out;
        } catch (SQLException error) {
            connection.rollback();
            throw error;
        } finally {
            connection.setTransactionIsolation(previousIsolation);
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    static String stringify(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    static String emptyChecksum() {
        return toHex(sha256().digest());
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
