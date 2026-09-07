package com.monkey.ktplus.storage.migration.phases;

import com.monkey.ktplus.storage.migration.MigrationContext;
import com.monkey.ktplus.storage.migration.MigrationPhase;
import com.monkey.ktplus.storage.migration.PhaseResult;
import java.util.Objects;
import java.util.logging.Logger;

public final class UnfreezePhase implements MigrationPhase {
    private final Logger logger;

    public UnfreezePhase(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public String name() {
        return "unfreeze";
    }

    @Override
    public PhaseResult run(MigrationContext context) {
        if (!context.writesWereFrozen() && !context.writeGate().writesFrozen()) {
            return PhaseResult.skipped("write gate was not frozen by this run");
        }
        context.writeGate().unfreezeWrites();
        context.addOperatorMessage("Write gate unfrozen");
        logger.info("[Migrate] Write gate unfrozen");
        return PhaseResult.success("writes unfrozen");
    }
}
