package com.monkey.ktplus.storage.migration;

import com.monkey.ktplus.storage.migration.report.MigrationReportWriter;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class MigrationPipeline {
    private final MigrationPhase preflight;
    private final MigrationPhase snapshot;
    private final MigrationPhase freeze;
    private final MigrationPhase export;
    private final MigrationPhase prepareTargetSchema;
    private final MigrationPhase importPhase;
    private final MigrationPhase validation;
    private final MigrationPhase configSwap;
    private final MigrationPhase unfreeze;
    private final MigrationLock migrationLock;
    private final Logger logger;

    public MigrationPipeline(
            MigrationPhase preflight,
            MigrationPhase snapshot,
            MigrationPhase freeze,
            MigrationPhase export,
            MigrationPhase prepareTargetSchema,
            MigrationPhase importPhase,
            MigrationPhase validation,
            MigrationPhase configSwap,
            MigrationPhase unfreeze,
            MigrationLock migrationLock,
            Logger logger) {
        this.preflight = Objects.requireNonNull(preflight, "preflight");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.freeze = Objects.requireNonNull(freeze, "freeze");
        this.export = Objects.requireNonNull(export, "export");
        this.prepareTargetSchema = Objects.requireNonNull(prepareTargetSchema, "prepareTargetSchema");
        this.importPhase = Objects.requireNonNull(importPhase, "importPhase");
        this.validation = Objects.requireNonNull(validation, "validation");
        this.configSwap = Objects.requireNonNull(configSwap, "configSwap");
        this.unfreeze = Objects.requireNonNull(unfreeze, "unfreeze");
        this.migrationLock = Objects.requireNonNull(migrationLock, "migrationLock");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public MigrationOutcome run(MigrationContext context) {
        Objects.requireNonNull(context, "context");
        boolean success = false;
        String message = "migration aborted";
        try {
            if (!runPhase(context, preflight)) {
                message = messageOf(context, preflight.name());
                return finalizeOutcome(context, false, message);
            }
            if (!runPhase(context, snapshot)) {
                message = messageOf(context, snapshot.name());
                return finalizeOutcome(context, false, message);
            }
            if (!runPhase(context, freeze)) {
                message = messageOf(context, freeze.name());
                return finalizeOutcome(context, false, message);
            }
            if (!runPhase(context, export)) {
                message = messageOf(context, export.name());
                return finalizeOutcome(context, false, message);
            }

            if (context.request().dryRun()) {
                context.recordPhase(
                        prepareTargetSchema.name(),
                        PhaseResult.skipped("dry-run: prepare-target-schema skipped"));
                context.recordPhase(importPhase.name(), PhaseResult.skipped("dry-run: import skipped"));
                context.recordPhase(validation.name(), PhaseResult.skipped("dry-run: validation skipped"));
                context.recordPhase(configSwap.name(), PhaseResult.skipped("dry-run: config-swap skipped"));
                context.addOperatorMessage(
                        "dry-run complete: no target writes and no database.type swap were performed");
                success = true;
                message = "dry-run completed without target writes or config swap";
                return finalizeOutcome(context, success, message);
            }

            if (!runPhase(context, prepareTargetSchema)) {
                message = messageOf(context, prepareTargetSchema.name());
                return finalizeOutcome(context, false, message);
            }
            if (!runPhase(context, importPhase)) {
                message = messageOf(context, importPhase.name());
                return finalizeOutcome(context, false, message);
            }
            if (!runPhase(context, validation)) {
                message = messageOf(context, validation.name());
                runPhase(context, configSwap);
                return finalizeOutcome(context, false, message);
            }
            if (!runPhase(context, configSwap)) {
                message = messageOf(context, configSwap.name());
                return finalizeOutcome(context, false, message);
            }
            success = true;
            message = "migration completed";
            return finalizeOutcome(context, success, message);
        } finally {
            cleanup(context);
        }
    }

    public MigrationOutcome runPostExport(MigrationContext context) {
        Objects.requireNonNull(context, "context");
        try {
            if (context.request().dryRun()) {
                context.recordPhase(
                        prepareTargetSchema.name(),
                        PhaseResult.skipped("dry-run: prepare-target-schema skipped"));
                context.recordPhase(importPhase.name(), PhaseResult.skipped("dry-run: import skipped"));
                context.recordPhase(validation.name(), PhaseResult.skipped("dry-run: validation skipped"));
                context.recordPhase(configSwap.name(), PhaseResult.skipped("dry-run: config-swap skipped"));
                return finalizeOutcome(context, true, "dry-run post-export");
            }
            if (!runPhase(context, prepareTargetSchema)) {
                return finalizeOutcome(context, false, messageOf(context, prepareTargetSchema.name()));
            }
            if (!runPhase(context, importPhase)) {
                return finalizeOutcome(context, false, messageOf(context, importPhase.name()));
            }
            if (!runPhase(context, validation)) {
                runPhase(context, configSwap);
                return finalizeOutcome(context, false, messageOf(context, validation.name()));
            }
            if (!runPhase(context, configSwap)) {
                return finalizeOutcome(context, false, messageOf(context, configSwap.name()));
            }
            return finalizeOutcome(context, true, "post-export completed");
        } finally {
            cleanup(context);
        }
    }

    private MigrationOutcome finalizeOutcome(MigrationContext context, boolean success, String message) {
        context.addOperatorMessage("pipeline-result: " + (success ? "SUCCESS" : "FAILED") + " — " + message);
        return new MigrationOutcome(success, message, null, context);
    }

    private void cleanup(MigrationContext context) {
        try {
            runPhase(context, unfreeze);
        } catch (Exception error) {
            logger.warning("[Migrate] Unfreeze failed: " + error.getMessage());
        }
        closeOwnedDataSources(context);
        if (context.ownsMigrationLock()) {
            migrationLock.release();
            context.clearOwnsMigrationLock();
        }
        try {
            MigrationReportWriter.write(context);
        } catch (Exception error) {
            logger.warning("[Migrate] Report write failed: " + error.getMessage());
        }
    }

    private boolean runPhase(MigrationContext context, MigrationPhase phase) {
        PhaseResult result = phase.run(context);
        context.recordPhase(phase.name(), result);
        return !result.failed();
    }

    private static String messageOf(MigrationContext context, String phase) {
        PhaseResult result = context.phaseResults().get(phase);
        return result == null ? phase + " failed" : result.message();
    }

    private static void closeOwnedDataSources(MigrationContext context) {
        closeIfOwned(context.targetDataSource(), context.ownsTargetDataSource());
        closeIfOwned(context.sourceDataSource(), context.ownsSourceDataSource());
    }

    private static void closeIfOwned(DataSource dataSource, boolean owned) {
        if (!owned || dataSource == null) {
            return;
        }
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }
}
