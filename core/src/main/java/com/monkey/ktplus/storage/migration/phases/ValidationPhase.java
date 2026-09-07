package com.monkey.ktplus.storage.migration.phases;

import com.monkey.ktplus.storage.migration.MigrationContext;
import com.monkey.ktplus.storage.migration.MigrationPhase;
import com.monkey.ktplus.storage.migration.PhaseResult;
import com.monkey.ktplus.storage.migration.transfer.MigrationTableCatalog;
import com.monkey.ktplus.storage.migration.validate.MigrationValidators;
import com.monkey.ktplus.storage.migration.validate.ValidationReport;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class ValidationPhase implements MigrationPhase {
    private final Logger logger;

    public ValidationPhase(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public String name() {
        return "validation";
    }

    @Override
    public PhaseResult run(MigrationContext context) {
        if (context.request().dryRun()) {
            context.markValidationResult(false, false);
            return PhaseResult.skipped("dry-run: validation skipped (no target import to validate)");
        }
        DataSource source = context.sourceDataSource();
        DataSource target = context.targetDataSource();
        if (source == null || target == null) {
            context.markValidationResult(false, true);
            return PhaseResult.failed("validation requires source and target DataSources");
        }
        try {
            ValidationReport report = MigrationValidators.validate(
                    source,
                    target,
                    context.exportedRowCounts(),
                    context.importedRowCounts(),
                    context.skippedRowCounts(),
                    MigrationTableCatalog.forTransfer(context.request()));
            for (String line : report.lines()) {
                context.addOperatorMessage(line);
            }
            context.markValidationResult(report.passed(), report.economicHardFail());
            if (!report.passed()) {
                logger.warning("[Migrate] Validation FAILED economicHardFail=" + report.economicHardFail());
                return PhaseResult.failed(
                        report.economicHardFail()
                                ? "validation HARD FAIL (economic) — ConfigSwap blocked"
                                : "validation failed");
            }
            logger.info("[Migrate] Validation OK");
            return PhaseResult.success("validation passed");
        } catch (Exception error) {
            context.markValidationResult(false, true);
            return PhaseResult.failed("validation failed: " + error.getMessage());
        }
    }
}
