package com.monkey.ktplus.storage.migration.snapshot;

import com.monkey.ktplus.storage.migration.MigrationDialect;
import com.monkey.ktplus.storage.migration.connection.MigrationEndpoint;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;

public final class SnapshotService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private SnapshotService() {}

    public static Path newSnapshotDirectory(Path backupsDirectory) throws IOException {
        Objects.requireNonNull(backupsDirectory, "backupsDirectory");
        Path snapshotDirectory = backupsDirectory.resolve("migration-snapshot-" + TS.format(LocalDateTime.now()));
        Files.createDirectories(snapshotDirectory);
        return snapshotDirectory;
    }

    
    public static @Nullable SnapshotResult createImmediateIfSafe(
            DataSource sourceDataSource,
            MigrationEndpoint sourceEndpoint,
            Path backupsDirectory)
            throws IOException, SQLException {
        Objects.requireNonNull(sourceDataSource, "sourceDataSource");
        Objects.requireNonNull(sourceEndpoint, "sourceEndpoint");
        Objects.requireNonNull(backupsDirectory, "backupsDirectory");

        if (sourceEndpoint.dialect() == MigrationDialect.SQLITE) {
            return null;
        }
        if (sourceEndpoint.dialect() == MigrationDialect.MYSQL) {
            Path snapshotDirectory = newSnapshotDirectory(backupsDirectory);
            return MysqlLogicalSnapshot.create(sourceDataSource, snapshotDirectory);
        }
        throw new IllegalArgumentException("unsupported snapshot dialect: " + sourceEndpoint.dialect());
    }
}
