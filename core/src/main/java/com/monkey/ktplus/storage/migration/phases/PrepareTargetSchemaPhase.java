package com.monkey.ktplus.storage.migration.phases;

import com.monkey.ktplus.logging.KtPlusLogging;
import com.monkey.ktplus.storage.migration.MigrationContext;
import com.monkey.ktplus.storage.migration.MigrationPhase;
import com.monkey.ktplus.storage.migration.PhaseResult;
import com.monkey.ktplus.storage.schema.FlywaySchemaMigrator;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.api.output.MigrateOutput;
import org.flywaydb.core.api.output.MigrateResult;

public final class PrepareTargetSchemaPhase implements MigrationPhase {
    private final Logger logger;

    public PrepareTargetSchemaPhase(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public String name() {
        return "prepare-target-schema";
    }

    @Override
    public PhaseResult run(MigrationContext context) {
        if (context.request().dryRun()) {
            return PhaseResult.skipped("dry-run: target schema not prepared (no target writes)");
        }
        DataSource target = context.targetDataSource();
        if (target == null) {
            return PhaseResult.failed("prepare-target-schema requires target DataSource");
        }
        try {
            context.markTargetWritesAttempted();
            MigrateResult migrateResult = new FlywaySchemaMigrator(
                            target, FlywaySchemaMigrator.class.getClassLoader())
                    .migrate();
            context.addOperatorMessage(
                    "Target schema prepared via Flyway (gate bypass: DDL/history only — see PrepareTargetSchemaPhase javadoc)");
            KtPlusLogging.success(logger, "Storage", "Target schema prepared via Flyway");
            KtPlusLogging.detail(
                    logger,
                    "Storage",
                    "executed="
                            + migrateResult.migrationsExecuted
                            + " | version="
                            + nullToDash(migrateResult.targetSchemaVersion));
            KtPlusLogging.detail(logger, "Storage", "databaseType=" + nullToDash(migrateResult.databaseType));
            if (migrateResult.getSuccessfulMigrations() != null) {
                for (MigrateOutput migration : migrateResult.getSuccessfulMigrations()) {
                    if (migration == null) {
                        continue;
                    }
                    KtPlusLogging.detail(
                            logger,
                            "Storage",
                            "applied v"
                                    + nullToDash(migration.version)
                                    + " -> "
                                    + nullToDash(migration.description));
                }
            }
            if (migrateResult.warnings != null) {
                for (String warning : migrateResult.warnings) {
                    if (warning == null || warning.isBlank()) {
                        continue;
                    }
                    KtPlusLogging.warn(logger, "Storage", "Flyway -> " + warning);
                }
            }
            return PhaseResult.success("target schema ready");
        } catch (Exception error) {
            return PhaseResult.failed("prepare-target-schema failed: " + error.getMessage());
        }
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
