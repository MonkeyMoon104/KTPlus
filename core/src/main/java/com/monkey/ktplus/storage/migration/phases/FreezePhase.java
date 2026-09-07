package com.monkey.ktplus.storage.migration.phases;

import com.monkey.ktplus.storage.DatabaseService;
import com.monkey.ktplus.storage.migration.MigrationContext;
import com.monkey.ktplus.storage.migration.MigrationDialect;
import com.monkey.ktplus.storage.migration.MigrationLock;
import com.monkey.ktplus.storage.migration.MigrationPhase;
import com.monkey.ktplus.storage.migration.PhaseResult;
import com.monkey.ktplus.storage.migration.connection.MigrationEndpoint;
import com.monkey.ktplus.storage.migration.snapshot.SnapshotResult;
import com.monkey.ktplus.storage.migration.snapshot.SqliteFileSnapshot;
import com.monkey.ktplus.storage.migration.writegate.StorageWriteGate;
import com.monkey.ktplus.storage.migration.writegate.WriteDrain;
import com.monkey.ktplus.storage.repository.TemporaryBlockRepository;
import java.time.Duration;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;

public final class FreezePhase implements MigrationPhase {
    private static final Duration DRAIN_TIMEOUT = Duration.ofSeconds(15);

    private final MigrationLock migrationLock;
    private final DatabaseService liveDatabase;
    private final @Nullable TemporaryBlockRepository temporaryBlocks;
    private final Logger logger;

    public FreezePhase(
            MigrationLock migrationLock,
            DatabaseService liveDatabase,
            @Nullable TemporaryBlockRepository temporaryBlocks,
            Logger logger) {
        this.migrationLock = Objects.requireNonNull(migrationLock, "migrationLock");
        this.liveDatabase = Objects.requireNonNull(liveDatabase, "liveDatabase");
        this.temporaryBlocks = temporaryBlocks;
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public String name() {
        return "freeze";
    }

    @Override
    public PhaseResult run(MigrationContext context) {
        StorageWriteGate gate = context.writeGate();

        if (gate.writesFrozen()) {
            return PhaseResult.failed(
                    "freeze refused: write gate is already frozen (unclean prior state); "
                            + "unfreeze or restart the plugin before migrating");
        }

        if (migrationLock.isHeld() && !context.ownsMigrationLock()) {
            return PhaseResult.failed(
                    "freeze refused: MigrationLock is held by another migration run");
        }
        if (!migrationLock.isHeld() || !context.ownsMigrationLock()) {
            return PhaseResult.failed(
                    "freeze refused: MigrationLock is not held by this migration run "
                            + "(acquire the lock before FreezePhase; do not rely on implicit single-flight alone)");
        }

        gate.freezeWrites();
        context.markWritesFrozen();
        context.addOperatorMessage("Write gate frozen");
        logger.info("[Migrate] Write gate frozen");

        try {
            WriteDrain.drain(liveDatabase, temporaryBlocks, DRAIN_TIMEOUT);
            context.addOperatorMessage("Write drain completed (in-flight + temp-block queue)");
            logger.info("[Migrate] Write drain completed");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return PhaseResult.failed("freeze drain interrupted/timed out: " + interrupted.getMessage());
        }

        MigrationEndpoint sourceEndpoint = context.sourceEndpoint();
        DataSource source = context.sourceDataSource();
        if (sourceEndpoint != null
                && source != null
                && sourceEndpoint.dialect() == MigrationDialect.SQLITE) {
            try {
                if (context.snapshotDirectory() == null) {
                    return PhaseResult.failed("sqlite snapshot directory was not reserved by SnapshotPhase");
                }
                SnapshotResult result = SqliteFileSnapshot.create(
                        source, sourceEndpoint, context.snapshotDirectory(), gate);
                context.setSnapshotManifest(result.manifest());
                SnapshotPhase.publishManifest(context, result);
                logger.info("[Migrate] SQLite post-freeze snapshot OK dir=" + result.directory());
            } catch (Exception error) {
                return PhaseResult.failed("sqlite post-freeze snapshot failed: " + error.getMessage());
            }
        }

        return PhaseResult.success("writes frozen and drained");
    }
}
