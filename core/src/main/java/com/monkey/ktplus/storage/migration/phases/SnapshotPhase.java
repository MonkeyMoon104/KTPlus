package com.monkey.ktplus.storage.migration.phases;

import com.monkey.ktplus.storage.migration.MigrationContext;
import com.monkey.ktplus.storage.migration.MigrationDialect;
import com.monkey.ktplus.storage.migration.MigrationPhase;
import com.monkey.ktplus.storage.migration.PhaseResult;
import com.monkey.ktplus.storage.migration.connection.MigrationEndpoint;
import com.monkey.ktplus.storage.migration.snapshot.SnapshotResult;
import com.monkey.ktplus.storage.migration.snapshot.SnapshotService;
import com.monkey.ktplus.storage.migration.transfer.TableFingerprint;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class SnapshotPhase implements MigrationPhase {
    private final Logger logger;

    public SnapshotPhase(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public String name() {
        return "snapshot";
    }

    @Override
    public PhaseResult run(MigrationContext context) {
        DataSource source = context.sourceDataSource();
        MigrationEndpoint sourceEndpoint = context.sourceEndpoint();
        if (source == null || sourceEndpoint == null) {
            return PhaseResult.failed("snapshot requires source DataSource and endpoint from preflight");
        }
        try {
            if (sourceEndpoint.dialect() == MigrationDialect.SQLITE) {
                
                context.setSnapshotDirectory(SnapshotService.newSnapshotDirectory(context.backupsDirectory()));
                context.addOperatorMessage(
                        "SQLite physical snapshot deferred until after freeze + drain "
                                + "(order: freeze → drain → WAL checkpoint → file copy)");
                context.addOperatorMessage("Reserved snapshot dir: " + context.snapshotDirectory());
                logger.info("[Migrate] SQLite snapshot deferred to post-freeze dir=" + context.snapshotDirectory());
                return PhaseResult.success("sqlite snapshot deferred to freeze phase");
            }

            SnapshotResult result =
                    SnapshotService.createImmediateIfSafe(source, sourceEndpoint, context.backupsDirectory());
            if (result == null) {
                return PhaseResult.failed("snapshot produced no artifact");
            }
            context.setSnapshotDirectory(result.directory());
            context.setSnapshotManifest(result.manifest());
            publishManifest(context, result);
            logger.info("[Migrate] Snapshot OK dir=" + result.directory() + " kind=" + result.manifest().kind());
            return PhaseResult.success("snapshot created at " + result.directory());
        } catch (Exception error) {
            return PhaseResult.failed("snapshot failed: " + error.getMessage());
        }
    }

    static void publishManifest(MigrationContext context, SnapshotResult result) {
        context.addOperatorMessage("Snapshot written to: " + result.directory());
        context.addOperatorMessage("Snapshot kind: " + result.manifest().kind());
        for (TableFingerprint fingerprint : result.manifest().tables().values()) {
            String line = "  "
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
    }
}
