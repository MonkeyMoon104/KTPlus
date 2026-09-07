package com.monkey.ktplus.storage.migration.snapshot;

import com.monkey.ktplus.storage.migration.MigrationDialect;
import com.monkey.ktplus.storage.migration.connection.MigrationEndpoint;
import com.monkey.ktplus.storage.migration.transfer.TableFingerprint;
import com.monkey.ktplus.storage.migration.transfer.TableFingerprinter;
import com.monkey.ktplus.storage.migration.writegate.StorageWriteGate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;

public final class SqliteFileSnapshot {
    private SqliteFileSnapshot() {}

    public static SnapshotResult create(
            DataSource sourceDataSource,
            MigrationEndpoint sourceEndpoint,
            Path snapshotDirectory,
            StorageWriteGate writeGate)
            throws IOException, SQLException {
        Objects.requireNonNull(sourceDataSource, "sourceDataSource");
        Objects.requireNonNull(sourceEndpoint, "sourceEndpoint");
        Objects.requireNonNull(snapshotDirectory, "snapshotDirectory");
        Objects.requireNonNull(writeGate, "writeGate");
        if (!writeGate.writesFrozen()) {
            throw new IllegalStateException(
                    "SqliteFileSnapshot requires an active write freeze before WAL checkpoint + file copy");
        }
        if (sourceEndpoint.dialect() != MigrationDialect.SQLITE) {
            throw new IllegalArgumentException("SqliteFileSnapshot requires SQLITE source endpoint");
        }
        Path sourceFile = sourceEndpoint.sqliteFile();
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new IOException("SQLite source file not found: " + sourceFile);
        }

        Files.createDirectories(snapshotDirectory);
        
        try (Connection connection = sourceDataSource.getConnection();
                Statement statement = connection.createStatement()) {
            try {
                statement.execute("PRAGMA wal_checkpoint(FULL)");
            } catch (SQLException ignored) {
                
            }
        }

        Path copied = snapshotDirectory.resolve(sourceFile.getFileName().toString());
        Files.copy(sourceFile, copied, StandardCopyOption.REPLACE_EXISTING);
        copySidecarIfPresent(sourceFile, snapshotDirectory, "-wal");
        copySidecarIfPresent(sourceFile, snapshotDirectory, "-shm");

        Map<String, TableFingerprint> fingerprints;
        try (Connection connection = sourceDataSource.getConnection()) {
            fingerprints = TableFingerprinter.fingerprintAllInOneTransaction(connection);
        }

        SnapshotManifest manifest = new SnapshotManifest(
                MigrationDialect.SQLITE,
                "sqlite-file-copy",
                System.currentTimeMillis(),
                fingerprints);
        manifest.writeTo(snapshotDirectory);
        return new SnapshotResult(snapshotDirectory, manifest);
    }

    private static void copySidecarIfPresent(Path sourceFile, Path snapshotDirectory, String suffix)
            throws IOException {
        Path sidecar = sourceFile.resolveSibling(sourceFile.getFileName().toString() + suffix);
        if (Files.isRegularFile(sidecar)) {
            Files.copy(
                    sidecar,
                    snapshotDirectory.resolve(sidecar.getFileName().toString()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
