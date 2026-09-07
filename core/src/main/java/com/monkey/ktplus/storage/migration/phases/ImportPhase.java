package com.monkey.ktplus.storage.migration.phases;

import com.monkey.ktplus.storage.migration.MigrationContext;
import com.monkey.ktplus.storage.migration.MigrationPhase;
import com.monkey.ktplus.storage.migration.PhaseResult;
import com.monkey.ktplus.storage.migration.transfer.ConflictPolicy;
import com.monkey.ktplus.storage.migration.transfer.MigrationTableCatalog;
import com.monkey.ktplus.storage.migration.transfer.TableImporter;
import com.monkey.ktplus.storage.migration.transfer.TableSpec;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class ImportPhase implements MigrationPhase {
    private final Logger logger;

    public ImportPhase(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public String name() {
        return "import";
    }

    @Override
    public PhaseResult run(MigrationContext context) {
        if (context.request().dryRun()) {
            return PhaseResult.skipped("dry-run: import skipped (no target writes)");
        }
        DataSource target = context.targetDataSource();
        Path exportDirectory = context.exportDirectory();
        if (target == null || exportDirectory == null) {
            return PhaseResult.failed("import requires target DataSource and export directory");
        }
        String requestedDialect = context.request().targetDialect().configValue();
        try (Connection connection = target.getConnection()) {
            context.markTargetWritesAttempted();
            String targetDialect = resolveInsertDialect(connection, requestedDialect);
            boolean previous = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (TableSpec spec : MigrationTableCatalog.forTransfer(context.request())) {
                    Path tsv = exportDirectory.resolve(spec.name() + ".tsv");
                    TableImporter.ImportCounts counts = TableImporter.importTable(
                            connection, spec, tsv, targetDialect, ConflictPolicy.SKIP_EXISTING);
                    context.putImportedRowCount(spec.name(), counts.inserted());
                    context.putSkippedRowCount(spec.name(), counts.skipped());
                    context.addOperatorMessage(
                            "imported "
                                    + spec.name()
                                    + " inserted="
                                    + counts.inserted()
                                    + " skipped="
                                    + counts.skipped());
                }
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(previous);
            }
            logger.info("[Migrate] Import OK skippedTotals=" + context.skippedRowCounts());
            return PhaseResult.success("import completed with SKIP_EXISTING policy");
        } catch (Exception error) {
            return PhaseResult.failed("import failed: " + error.getMessage());
        }
    }

    private static String resolveInsertDialect(Connection connection, String requested) {
        try {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product != null) {
                String lower = product.toLowerCase(java.util.Locale.ROOT);
                if (lower.contains("sqlite")) {
                    return "sqlite";
                }
                if (lower.contains("mysql")) {
                    return "mysql";
                }
            }
        } catch (Exception ignored) {
            
        }
        return requested;
    }
}
