package com.monkey.ktplus.storage.migration.phases;

import com.monkey.ktplus.storage.migration.MigrationContext;
import com.monkey.ktplus.storage.migration.MigrationPhase;
import com.monkey.ktplus.storage.migration.PhaseResult;
import com.monkey.ktplus.storage.migration.transfer.TableExporter;
import com.monkey.ktplus.storage.migration.transfer.TableFingerprint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class ExportPhase implements MigrationPhase {
    private final Logger logger;

    public ExportPhase(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public String name() {
        return "export";
    }

    @Override
    public PhaseResult run(MigrationContext context) {
        if (!context.writeGate().writesFrozen()) {
            return PhaseResult.failed("export refused: write gate is not frozen");
        }
        DataSource source = context.sourceDataSource();
        if (source == null) {
            return PhaseResult.failed("export requires source DataSource");
        }
        try {
            Path exportDirectory = context.exportDirectory();
            if (exportDirectory == null) {
                exportDirectory = context.backupsDirectory().resolve("migration-export");
                Files.createDirectories(exportDirectory);
                context.setExportDirectory(exportDirectory);
            }
            Map<String, TableFingerprint> fingerprints;
            try (Connection connection = source.getConnection()) {
                fingerprints = TableExporter.exportForTransfer(connection, exportDirectory, context.request());
            }
            for (TableFingerprint fingerprint : fingerprints.values()) {
                context.putExportedRowCount(fingerprint.tableName(), fingerprint.rowCount());
                String line = "exported "
                        + fingerprint.tableName()
                        + " rows="
                        + fingerprint.rowCount()
                        + " checksum="
                        + fingerprint.contentChecksum();
                if (fingerprint.sumBalance() != null) {
                    line += " sumBalance=" + fingerprint.sumBalance();
                }
                context.addOperatorMessage(line);
            }
            context.setExportFingerprints(fingerprints);
            logger.info("[Migrate] Export OK dir=" + exportDirectory + " tables=" + fingerprints.size());
            return PhaseResult.success("exported " + fingerprints.size() + " tables to " + exportDirectory);
        } catch (Exception error) {
            return PhaseResult.failed("export failed: " + error.getMessage());
        }
    }
}
