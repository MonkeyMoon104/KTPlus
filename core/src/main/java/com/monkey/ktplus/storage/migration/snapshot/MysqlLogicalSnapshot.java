package com.monkey.ktplus.storage.migration.snapshot;

import com.monkey.ktplus.storage.migration.MigrationDialect;
import com.monkey.ktplus.storage.migration.transfer.TableFingerprint;
import com.monkey.ktplus.storage.migration.transfer.TableExporter;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;

public final class MysqlLogicalSnapshot {
    private MysqlLogicalSnapshot() {}

    public static SnapshotResult create(DataSource sourceDataSource, Path snapshotDirectory)
            throws IOException, SQLException {
        Objects.requireNonNull(sourceDataSource, "sourceDataSource");
        Objects.requireNonNull(snapshotDirectory, "snapshotDirectory");
        Path tablesDir = snapshotDirectory.resolve("tables");
        Map<String, TableFingerprint> fingerprints;
        try (Connection connection = sourceDataSource.getConnection()) {
            fingerprints = TableExporter.exportSnapshot(connection, tablesDir);
        }
        SnapshotManifest manifest = new SnapshotManifest(
                MigrationDialect.MYSQL,
                "mysql-logical-tsv",
                System.currentTimeMillis(),
                fingerprints);
        manifest.writeTo(snapshotDirectory);
        return new SnapshotResult(snapshotDirectory, manifest);
    }
}
