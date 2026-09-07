package com.monkey.ktplus.storage.migration.phases;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.storage.DatabaseService;
import com.monkey.ktplus.storage.migration.MigrationContext;
import com.monkey.ktplus.storage.migration.MigrationDialect;
import com.monkey.ktplus.storage.migration.MigrationPhase;
import com.monkey.ktplus.storage.migration.MigrationRequest;
import com.monkey.ktplus.storage.migration.PhaseResult;
import com.monkey.ktplus.storage.migration.connection.MigrationDataSourceFactory;
import com.monkey.ktplus.storage.migration.connection.MigrationEndpoint;
import com.monkey.ktplus.storage.migration.connection.MigrationJdbcUrls;
import com.monkey.ktplus.storage.migration.connection.TargetMySqlInspector;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.bukkit.plugin.java.JavaPlugin;

public final class PreflightPhase implements MigrationPhase {
    private final JavaPlugin plugin;
    private final ConfigSnapshot config;
    private final DatabaseService liveDatabase;
    private final MigrationDataSourceFactory dataSourceFactory;
    private final Logger logger;

    public PreflightPhase(
            JavaPlugin plugin,
            ConfigSnapshot config,
            DatabaseService liveDatabase,
            MigrationDataSourceFactory dataSourceFactory,
            Logger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.liveDatabase = Objects.requireNonNull(liveDatabase, "liveDatabase");
        this.dataSourceFactory = Objects.requireNonNull(dataSourceFactory, "dataSourceFactory");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public String name() {
        return "preflight";
    }

    @Override
    public PhaseResult run(MigrationContext context) {
        MigrationRequest request = context.request();
        String configuredType = config.database().getString("database.type", "sqlite");
        if (configuredType == null) {
            configuredType = "sqlite";
        }
        String normalizedConfigured = configuredType.trim().toLowerCase(Locale.ROOT);
        if (!MigrationDialect.isSupportedConfigType(normalizedConfigured)) {
            return PhaseResult.failed(
                    "migrator supports only sqlite<->mysql in this version (current database.type="
                            + configuredType
                            + ")");
        }

        MigrationDialect liveDialect;
        try {
            liveDialect = MigrationDialect.parse(normalizedConfigured);
        } catch (IllegalArgumentException error) {
            return PhaseResult.failed(error.getMessage());
        }
        if (liveDialect != request.sourceDialect()) {
            return PhaseResult.failed(
                    "request source dialect "
                            + request.sourceDialect().configValue()
                            + " does not match live database.type "
                            + liveDialect.configValue());
        }

        MigrationEndpoint sourceEndpoint =
                MigrationJdbcUrls.resolveEndpoint(plugin, config, request.sourceDialect());
        MigrationEndpoint targetEndpoint =
                MigrationJdbcUrls.resolveEndpoint(plugin, config, request.targetDialect());
        context.setSourceEndpoint(sourceEndpoint);
        context.setTargetEndpoint(targetEndpoint);

        publishSummary(context, sourceEndpoint, targetEndpoint);

        if (!targetEndpoint.matchesConfirmation(request.typedTargetConfirmation())) {
            return PhaseResult.failed(
                    "target confirmation mismatch: type exactly this token from the summary: "
                            + targetEndpoint.confirmationToken());
        }

        try {
            context.setSourceDataSource(liveDatabase.dataSource(), false);
            probeSource(context.sourceDataSource());

            HikariDataSource targetDataSource = dataSourceFactory.open(config, targetEndpoint);
            context.setTargetDataSource(targetDataSource, true);
            if (request.targetDialect() == MigrationDialect.MYSQL) {
                TargetMySqlInspector.MySqlInspection inspection = TargetMySqlInspector.inspect(targetDataSource);
                context.addOperatorMessage(
                        "Target MySQL version="
                                + inspection.version()
                                + " charset="
                                + inspection.charset()
                                + " collation="
                                + inspection.collation());
                if (inspection.nonemptyApplicationData() && !request.allowNonemptyTarget()) {
                    return PhaseResult.failed(
                            "target already contains kt_* application rows; re-run with --allow-nonempty-target to continue (skip-existing policy)");
                }
                if (inspection.nonemptyApplicationData()) {
                    context.addWarning("target is non-empty; import will skip existing primary keys");
                }
            } else if (countApplicationRows(targetDataSource) > 0L && !request.allowNonemptyTarget()) {
                return PhaseResult.failed(
                        "target SQLite already contains kt_* application rows; re-run with --allow-nonempty-target");
            }

            long pendingInventory = countPendingInventory(context.sourceDataSource());
            context.addOperatorMessage("Pending inventory rows on source: " + pendingInventory);
            if (pendingInventory > 0L && !request.forcePendingInventory()) {
                return PhaseResult.failed(
                        "source has pending inventory rows; close GUI sessions or re-run with --force-pending-inventory");
            }
            if (pendingInventory > 0L) {
                context.addWarning("migrating pending inventory because --force-pending-inventory was set");
            }

            logger.info("[Migrate] Preflight OK source="
                    + sourceEndpoint.displaySummary()
                    + " target="
                    + targetEndpoint.displaySummary());
            return PhaseResult.success("preflight passed");
        } catch (Exception error) {
            return PhaseResult.failed("preflight failed: " + error.getMessage());
        }
    }

    private void publishSummary(
            MigrationContext context, MigrationEndpoint sourceEndpoint, MigrationEndpoint targetEndpoint) {
        context.addOperatorMessage("=== Migration endpoint summary (passwords never shown) ===");
        context.addOperatorMessage("SOURCE: " + sourceEndpoint.displaySummary());
        context.addOperatorMessage("TARGET: " + targetEndpoint.displaySummary());
        context.addOperatorMessage(
                "Confirm TARGET by typing exactly: " + targetEndpoint.confirmationToken());
        logger.info("[Migrate] SOURCE: " + sourceEndpoint.displaySummary());
        logger.info("[Migrate] TARGET: " + targetEndpoint.displaySummary());
        logger.info("[Migrate] Confirm TARGET token: " + targetEndpoint.confirmationToken());
    }

    private static void probeSource(DataSource source) throws Exception {
        try (Connection connection = source.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            if (!resultSet.next()) {
                throw new IllegalStateException("source connection probe failed");
            }
        }
    }

    private static long countPendingInventory(DataSource source) throws Exception {
        try (Connection connection = source.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM kt_pending_inventory")) {
            if (!resultSet.next()) {
                return 0L;
            }
            return resultSet.getLong(1);
        } catch (Exception error) {
            return 0L;
        }
    }

    private static long countApplicationRows(DataSource dataSource) throws Exception {
        String[] tables = {
            "kt_killcoins",
            "kt_purchases",
            "kt_player_effects",
            "kt_review_claims",
            "kt_pending_inventory",
            "kt_temp_blocks"
        };
        long total = 0L;
        try (Connection connection = dataSource.getConnection()) {
            for (String table : tables) {
                try (Statement statement = connection.createStatement();
                        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (resultSet.next()) {
                        total += resultSet.getLong(1);
                    }
                } catch (Exception ignored) {
                    
                }
            }
        }
        return total;
    }
}
