package com.monkey.ktplus.storage.migration.phases;

import com.monkey.ktplus.storage.migration.DatabaseTypeSwapper;
import com.monkey.ktplus.storage.migration.MigrationContext;
import com.monkey.ktplus.storage.migration.MigrationPhase;
import com.monkey.ktplus.storage.migration.PhaseResult;
import java.util.Objects;
import java.util.logging.Logger;

public final class ConfigSwapPhase implements MigrationPhase {
    private final DatabaseTypeSwapper typeSwapper;
    private final Logger logger;

    public ConfigSwapPhase(DatabaseTypeSwapper typeSwapper, Logger logger) {
        this.typeSwapper = Objects.requireNonNull(typeSwapper, "typeSwapper");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public String name() {
        return "config-swap";
    }

    @Override
    public PhaseResult run(MigrationContext context) {
        if (context.request().dryRun()) {
            return PhaseResult.skipped("dry-run: config-swap skipped");
        }
        if (context.economicValidationHardFailed()) {
            return PhaseResult.failed(
                    "config-swap refused: economic validation HARD FAIL (kt_killcoins/kt_purchases); "
                            + "database.type was NOT changed");
        }
        if (!context.validationPassed()) {
            return PhaseResult.failed(
                    "config-swap refused: validation did not pass; database.type was NOT changed");
        }
        try {
            String targetType = context.request().targetDialect().configValue();
            typeSwapper.saveDatabaseType(targetType);
            context.markConfigSwapped(true);
            context.addOperatorMessage(
                    "database.type swapped to "
                            + targetType
                            + " — restart the server to use the new dialect");
            logger.info("[Migrate] database.type swapped to " + targetType);
            return PhaseResult.success("database.type=" + targetType + " (restart required)");
        } catch (Exception error) {
            context.markConfigSwapped(false);
            return PhaseResult.failed("config-swap failed: " + error.getMessage());
        }
    }
}
