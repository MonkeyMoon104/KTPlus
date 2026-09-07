package com.monkey.ktplus.logging;

import org.flywaydb.core.api.output.MigrateOutput;
import org.flywaydb.core.api.output.MigrateResult;
import java.util.List;

public final class FlywayBootReport {
    private FlywayBootReport() {}

    public static void log(BootLogger boot, MigrateResult result) {
        if (result == null) {
            boot.warn("Storage", "Flyway returned no result");
            return;
        }
        boot.detail(
                "Storage",
                "Flyway database -> "
                        + nullToDash(result.databaseType)
                        + " | flyway="
                        + nullToDash(result.flywayVersion));
        boot.detail(
                "Storage",
                "Schema version -> "
                        + nullToDash(result.initialSchemaVersion)
                        + " -> "
                        + nullToDash(result.targetSchemaVersion));
        boot.detail(
                "Storage",
                "Migrations executed -> "
                        + result.migrationsExecuted
                        + " | success="
                        + result.success);

        List<MigrateOutput> applied = result.getSuccessfulMigrations();
        if (applied != null) {
            for (MigrateOutput migration : applied) {
                if (migration == null) {
                    continue;
                }
                boot.detail(
                        "Storage",
                        "Applied v"
                                + nullToDash(migration.version)
                                + " -> "
                                + nullToDash(migration.description)
                                + " ("
                                + migration.executionTime
                                + "ms)");
            }
        }

        if (result.warnings != null) {
            for (String warning : result.warnings) {
                if (warning == null || warning.isBlank()) {
                    continue;
                }
                boot.warn("Storage", "Flyway -> " + warning);
            }
        }
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
