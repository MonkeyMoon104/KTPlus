package com.monkey.ktplus.storage.migration;

public interface MigrationPhase {
    String name();

    PhaseResult run(MigrationContext context);
}
